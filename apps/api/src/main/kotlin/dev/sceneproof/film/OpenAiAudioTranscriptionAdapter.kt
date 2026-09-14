package dev.sceneproof.film

import dev.sceneproof.analysis.AnalysisFailure
import dev.sceneproof.analysis.BoundedResponseBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.json.JsonFactory
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToLong

@Component
class OpenAiAudioTranscriptionAdapter(@param:Value("\${sceneproof.astra.api-key:}") private val apiKey: String) : AudioTranscriptionPort {
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build()
    private val strict = JsonMapper.builder(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build()).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build()

    override fun transcribe(input: AudioInput): TranscriptionCompletion {
        if (apiKey.isBlank()) throw AnalysisFailure("TRANSCRIPTION_NOT_CONFIGURED", "Server-side OpenAI configuration is missing.", 503)
        val boundary = "SceneProof${UUID.randomUUID()}"
        val responseInfo = AtomicReference<HttpResponse.ResponseInfo>()
        val pending = client.sendAsync(request(input, boundary)) { info -> responseInfo.set(info); BoundedResponseBody() }
        try {
            val response = pending.get(120, TimeUnit.SECONDS)
            val requestId = safeRequestId(response.headers().firstValue("x-request-id").orElse(null))
            if (response.statusCode() !in 200..299) throw rejection(response.statusCode(), response.body(), requestId)
            return parse(response.body(), input.durationMs, requestId)
        } catch (failure: AnalysisFailure) { throw failure }
        catch (_: InterruptedException) {
            pending.cancel(true)
            Thread.currentThread().interrupt()
            throw incompleteResponse(responseInfo.get(), interrupted = true)
        } catch (_: Exception) {
            pending.cancel(true)
            throw incompleteResponse(responseInfo.get())
        }
    }

    internal fun request(input: AudioInput, boundary: String): HttpRequest = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/audio/transcriptions"))
        .timeout(Duration.ofSeconds(120)).header("Authorization", "Bearer $apiKey")
        .header("Content-Type", "multipart/form-data; boundary=$boundary")
        .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody(input, boundary))).build()

    internal fun incompleteResponse(info: HttpResponse.ResponseInfo?, interrupted: Boolean = false): AnalysisFailure {
        if (info != null && info.statusCode() !in 200..299) return rejection(info.statusCode(), ByteArray(0), info.headers().firstValue("x-request-id").orElse(null))
        return if (interrupted) AnalysisFailure("TRANSCRIPTION_INTERRUPTED", "Transcription was interrupted. No paid request was replayed.", 503)
        else AnalysisFailure("TRANSCRIPTION_UNAVAILABLE", "Transcription timed out or could not be reached. No automatic retry was made.", 503)
    }

    internal fun rejection(status: Int, bytes: ByteArray, requestId: String?): AnalysisFailure {
        require(status in 100..999 && status !in 200..299)
        val error = if (bytes.size <= 8192) try { strict.readTree(bytes).takeIf { it.isObject }?.path("error")?.takeIf { it.isObject } } catch (_: Exception) { null } else null
        fun allowed(field: String, values: Set<String>): String? = error?.path(field)?.takeIf { it.isString }?.asString()
            ?.takeIf { it in values && (apiKey.isEmpty() || !it.contains(apiKey)) }
        val type = allowed("type", safeTypes) ?: "unavailable"
        val code = allowed("code", safeCodes) ?: "unavailable"
        val message = allowed("message", safeMessages) ?: "Provider message withheld; no safe allowlisted message was available."
        val detail = "OpenAI transcription failed (HTTP $status; type=$type; code=$code). $message No automatic retry was made."
        require(detail.length <= 500)
        return AnalysisFailure("TRANSCRIPTION_REJECTED", detail, 502, providerRequestId = safeRequestId(requestId))
    }

    private fun safeRequestId(value: String?): String? = value?.takeIf {
        it.matches(Regex("req_[A-Za-z0-9_-]{1,156}")) && (apiKey.isEmpty() || !it.contains(apiKey)) && !it.contains("sk-")
    }

    private companion object {
        val safeTypes = setOf("invalid_request_error", "authentication_error", "permission_error", "rate_limit_error", "server_error", "api_error", "insufficient_quota", "requests", "tokens")
        val safeCodes = setOf("invalid_api_key", "insufficient_quota", "rate_limit_exceeded", "model_not_found", "invalid_value", "missing_required_parameter", "unsupported_value", "unsupported_parameter", "invalid_parameter", "invalid_file_format", "invalid_file", "audio_too_large", "file_too_large", "permission_denied", "access_denied", "access_terminated", "organization_restricted", "project_not_found", "invalid_project", "billing_hard_limit_reached", "internal_error", "server_error", "service_unavailable", "request_timeout")
        val safeMessages = setOf("Invalid file format.", "Unsupported file format.", "The audio file is too large.", "You exceeded your current quota, please check your plan and billing details.", "The server had an error while processing your request. Sorry about that!", "Rate limit exceeded.", "Internal server error.", "Service unavailable.")
    }

    internal fun requestBody(input: AudioInput, boundary: String): ByteArray {
        require(input.wav.size in 45..4_000_000 && input.durationMs in 1..120000)
        require(boundary.matches(Regex("[a-zA-Z0-9-]{1,80}")))
        val output = ByteArrayOutputStream()
        fun text(value: String) { output.write(value.toByteArray(Charsets.UTF_8)) }
        listOf("model" to "whisper-1", "response_format" to "verbose_json", "timestamp_granularities[]" to "segment").forEach { (name, value) ->
            text("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n")
        }
        text("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"source.wav\"\r\nContent-Type: audio/wav\r\n\r\n")
        output.write(input.wav)
        text("\r\n--$boundary--\r\n")
        return output.toByteArray()
    }

    internal fun parse(bytes: ByteArray, durationMs: Long, requestId: String?): TranscriptionCompletion {
        try {
            require(bytes.size <= 256 * 1024)
            val root = strict.readTree(bytes)
            require(root.isObject && root.path("text").isString && root.path("text").asString().length <= 24000)
            val values = root.path("segments")
            require(values.isArray && values.size() <= 200)
            val segments = values.asSequence().map { segment ->
                require(segment.path("start").isNumber && segment.path("end").isNumber && segment.path("text").isString)
                val start = segment.path("start").asDouble() * 1000
                val end = segment.path("end").asDouble() * 1000
                require(start.isFinite() && end.isFinite() && start >= 0 && end > start && end <= durationMs)
                val text = segment.path("text").asString().trim()
                require(text.isNotEmpty() && text.length <= 2000)
                TranscribedSegment(start.roundToLong(), end.roundToLong(), text).also { require(it.endMs > it.startMs) }
            }.toList()
            require(segments.zipWithNext().all { it.first.startMs <= it.second.startMs })
            require(segments.sumOf { it.text.length } <= 24000)
            require(segments.isNotEmpty() || root.path("text").asString().isBlank())
            return TranscriptionCompletion(segments, requestId)
        } catch (_: Exception) { throw AnalysisFailure("INVALID_TRANSCRIPTION", "The transcript or its source timestamps were invalid. No Film Understanding call was made.", 502, providerRequestId = requestId) }
    }
}
