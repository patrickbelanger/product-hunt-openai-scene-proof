package dev.sceneproof.analysis

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Flow
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal class OpenAiResponsesTransport(private val mapper: ObjectMapper, private val apiKey: String) {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build()
    internal fun send(body: ByteArray): HttpResponse<ByteArray> {
        if (apiKey.isBlank()) throw AnalysisFailure("ANALYSIS_NOT_CONFIGURED", "Server-side OpenAI configuration is missing.", 503)
        val request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
            .timeout(Duration.ofSeconds(120)).header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofByteArray(body)).build()
        val pending = client.sendAsync(request) { BoundedResponseBody() }
        val response = try {
            pending.get(120, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            pending.cancel(true)
            throw AnalysisFailure("PROVIDER_TIMEOUT", "OpenAI did not complete within 120 seconds. This request will not be retried automatically.", 504)
        } catch (_: InterruptedException) {
            pending.cancel(true)
            Thread.currentThread().interrupt()
            throw AnalysisFailure("ANALYSIS_INTERRUPTED", "The analysis was interrupted. No findings were saved.", 503)
        } catch (exception: java.util.concurrent.ExecutionException) {
            throw transportFailure(exception.cause)
        }
        return response
    }

    internal fun <Completion> parseEnvelope(status: Int, bytes: ByteArray, requestId: String?, decode: (String, String?, String?, AnalysisUsage?) -> Completion): Completion {
        val safeRequestId = safeIdentifier(requestId, "req_")
        if (status !in 200..299) {
            val failure = when (status) {
                401, 403 -> AnalysisFailure("PROVIDER_AUTHENTICATION", "OpenAI authentication or access failed. Check server configuration.", 503)
                429 -> AnalysisFailure("PROVIDER_RATE_LIMITED", "OpenAI rate or credit limits were reached. Check account limits before retrying.", 429)
                in 500..599 -> AnalysisFailure("PROVIDER_UNAVAILABLE", "OpenAI is temporarily unavailable. No automatic retry was made.", 503)
                else -> AnalysisFailure("PROVIDER_REJECTED_REQUEST", "OpenAI rejected the analysis request. Check the server integration.", 502)
            }
            throw AnalysisFailure(failure.code, failure.detail, failure.httpStatus, providerRequestId = safeRequestId)
        }
        var usage: AnalysisUsage? = null
        var responseId: String? = null
        try {
            require(bytes.size <= 256 * 1024)
            val root = mapper.readTree(bytes)
            usage = readUsage(root["usage"])
            responseId = safeIdentifier(root["id"]?.takeIf { it.isString }?.asString(), "resp_")
            if (root["status"]?.asString() != "completed") throw AnalysisFailure("PROVIDER_INCOMPLETE", "OpenAI did not return a complete analysis. No findings were saved.", 502)
            require(root["model"]?.asString() == ANALYSIS_MODEL)
            val output = root["output"]
            require(output != null && output.isArray)
            val messages = output.filter { it["type"]?.asString() == "message" }
            require(messages.size == 1 && messages.single()["status"]?.asString() == "completed")
            val contents = messages.single()["content"]
            require(contents != null && contents.isArray)
            if (contents.any { it["type"]?.asString() == "refusal" }) throw AnalysisFailure("PROVIDER_REFUSAL", "OpenAI declined this analysis. No findings were saved.", 422)
            require(contents.size() == 1 && contents[0]["type"]?.asString() == "output_text" && contents[0]["text"].isString)
            return decode(contents[0]["text"].asString(), responseId, safeRequestId, usage)
        } catch (exception: Exception) {
            val failure = exception as? AnalysisFailure ?: AnalysisFailure("INVALID_MODEL_OUTPUT", "OpenAI returned an invalid analysis. No findings were saved.", 502)
            throw AnalysisFailure(failure.code, failure.detail, failure.httpStatus, usage, responseId, safeRequestId)
        }
    }

    private fun safeIdentifier(value: String?, prefix: String): String? = value?.takeIf {
        it.matches(Regex("${prefix}[A-Za-z0-9_-]{1,155}")) && !it.contains("sk-") && (apiKey.isEmpty() || !it.contains(apiKey))
    }

    private fun readUsage(node: JsonNode?): AnalysisUsage? {
        if (node == null || node.isNull) return null
        require(node.isObject)
        fun tokens(value: JsonNode?): Long? {
            if (value == null || value.isNull) return null
            require(value.isIntegralNumber && value.canConvertToLong() && value.asLong() >= 0)
            return value.asLong()
        }
        return AnalysisUsage(tokens(node["input_tokens"]), tokens(node["output_tokens"]), tokens(node["total_tokens"]),
            tokens(node["input_tokens_details"]?.get("cached_tokens")), tokens(node["output_tokens_details"]?.get("reasoning_tokens")),
            tokens(node["input_tokens_details"]?.get("cache_write_tokens")))
    }

    internal fun transportFailure(cause: Throwable?): AnalysisFailure = when (cause) {
        is java.net.http.HttpTimeoutException -> AnalysisFailure("PROVIDER_TIMEOUT", "OpenAI timed out. No automatic retry was made.", 504)
        is AnalysisFailure -> cause
        else -> AnalysisFailure("PROVIDER_UNAVAILABLE", "OpenAI could not be reached. No automatic retry was made.", 503)
    }
}

internal class BoundedResponseBody : HttpResponse.BodySubscriber<ByteArray> {
    private val completion = CompletableFuture<ByteArray>()
    private val buffer = ByteArrayOutputStream()
    private lateinit var subscription: Flow.Subscription
    override fun getBody(): CompletionStage<ByteArray> = completion
    override fun onSubscribe(subscription: Flow.Subscription) { this.subscription = subscription; subscription.request(1) }
    override fun onNext(items: List<ByteBuffer>) {
        for (item in items) {
            if (buffer.size().toLong() + item.remaining() > 256 * 1024) {
                subscription.cancel()
                completion.completeExceptionally(AnalysisFailure("PROVIDER_RESPONSE_TOO_LARGE", "OpenAI returned an oversized response.", 502))
                return
            }
            val bytes = ByteArray(item.remaining())
            item.get(bytes)
            buffer.write(bytes)
        }
        subscription.request(1)
    }
    override fun onError(error: Throwable) { completion.completeExceptionally(error) }
    override fun onComplete() { completion.complete(buffer.toByteArray()) }
}
