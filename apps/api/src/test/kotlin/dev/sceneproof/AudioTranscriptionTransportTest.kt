package dev.sceneproof

import com.sun.net.httpserver.HttpServer
import dev.sceneproof.analysis.AnalysisFailure
import dev.sceneproof.film.AudioInput
import dev.sceneproof.film.OpenAiAudioTranscriptionAdapter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.URI
import java.net.UnknownHostException
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.nio.channels.UnresolvedAddressException
import java.util.concurrent.ExecutionException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLHandshakeException
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

class AudioTranscriptionTransportTest {
    private val adapter = OpenAiAudioTranscriptionAdapter("test-key")
    private data class Captured(val method: String, val path: String, val contentType: String, val contentLength: Int, val body: ByteArray)

    private fun wav(durationMs: Long = 1000): ByteArray {
        val pcm = ByteArray((durationMs * 32).toInt()) { (it % 127).toByte() }
        val output = ByteArrayOutputStream()
        AudioInputStream(ByteArrayInputStream(pcm), AudioFormat(16000f, 16, 1, true, false), durationMs * 16).use {
            AudioSystem.write(it, AudioFileFormat.Type.WAVE, output)
        }
        return output.toByteArray()
    }

    @ParameterizedTest
    @ValueSource(longs = [1000, 120000])
    fun `production dispatch delivers a complete binary WAV multipart to audio endpoint only`(durationMs: Long) {
        val captured = LinkedBlockingQueue<Captured>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val bytes = exchange.requestBody.use { it.readAllBytes() }
            captured.add(Captured(exchange.requestMethod, exchange.requestURI.toString(), exchange.requestHeaders.getFirst("Content-Type"), exchange.requestHeaders.getFirst("Content-Length").toInt(), bytes))
            val response = """{"text":"Deterministic narration","duration":1,"task":"transcribe","segments":[{"id":"s0","type":"transcript.text.segment","speaker":"untrusted label","start":0.125,"end":0.9,"text":"Deterministic narration"}]}""".toByteArray()
            exchange.responseHeaders.add("x-request-id", "req_local_capture")
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        try {
            val local = OpenAiAudioTranscriptionAdapter.forLocalCapture("test-key", URI("http://127.0.0.1:${server.address.port}/v1/audio/transcriptions"))
            val audio = AudioInput(wav(durationMs), durationMs)
            val result = local.transcribe(audio)
            assertThat(result.providerRequestId).isEqualTo("req_local_capture")
            assertThat(result.segments.single().startMs).isEqualTo(125)
            assertThat(result.segments.single().endMs).isEqualTo(900)
            assertThat(result.toString()).doesNotContain("untrusted label")
            val request = requireNotNull(captured.poll(2, TimeUnit.SECONDS))
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.path).isEqualTo("/v1/audio/transcriptions")
            assertThat(request.contentType).startsWith("multipart/form-data; boundary=SceneProof")
            val boundary = request.contentType.substringAfter("boundary=")
            assertThat(request.body).isEqualTo(local.requestBody(audio, boundary))
            assertThat(request.contentLength).isEqualTo(request.body.size)
            val body = request.body.toString(Charsets.ISO_8859_1)
            assertThat(body).contains("name=\"model\"\r\n\r\ngpt-4o-transcribe-diarize\r\n", "name=\"response_format\"\r\n\r\ndiarized_json\r\n", "name=\"chunking_strategy\"\r\n\r\nauto\r\n", "name=\"file\"; filename=\"source.wav\"\r\nContent-Type: audio/wav\r\n\r\nRIFF")
            assertThat(body).doesNotContain("timestamp_granularities", "verbose_json", "gpt-6-astra", "prompt", "keywords")
            assertThat(captured).isEmpty()
        } finally { server.stop(0) }
    }

    @Test fun `transcription has no Responses Chat arbitrary origin or redirect escape hatch`() {
        val input = AudioInput(wav(), 1000)
        assertThat(adapter.request(input, "boundary").uri()).isEqualTo(URI("https://api.openai.com/v1/audio/transcriptions"))
        listOf("http://127.0.0.1:1234/v1/responses", "http://127.0.0.1:1234/v1/chat/completions", "https://api.openai.com/v1/audio/transcriptions", "http://example.com:1234/v1/audio/transcriptions", "http://127.0.0.1:1234/v1/audio/transcriptions?route=responses", "http://secret@127.0.0.1:1234/v1/audio/transcriptions").forEach { uri ->
            assertThatThrownBy { OpenAiAudioTranscriptionAdapter.forLocalCapture("test-key", URI(uri)) }.isInstanceOf(IllegalArgumentException::class.java)
        }
        val requests = LinkedBlockingQueue<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.add(exchange.requestURI.path)
            exchange.requestBody.use { it.readAllBytes() }
            exchange.responseHeaders.add("Location", "/v1/responses")
            exchange.sendResponseHeaders(307, -1)
            exchange.close()
        }
        server.start()
        try {
            val local = OpenAiAudioTranscriptionAdapter.forLocalCapture("test-key", URI("http://127.0.0.1:${server.address.port}/v1/audio/transcriptions"))
            assertThatThrownBy { local.transcribe(input) }.isInstanceOfSatisfying(AnalysisFailure::class.java) { assertThat(it.detail).contains("HTTP 307") }
            assertThat(requests.toList()).containsExactly("/v1/audio/transcriptions")
        } finally { server.stop(0) }
    }

    @Test fun `nested local transport categories never expose raw causes messages or suppressed content`() {
        val secret = "Bearer test-key /private/path raw transcript"
        val causes = listOf(
            UnknownHostException(secret) to "DNS_FAILURE", UnresolvedAddressException() to "DNS_FAILURE",
            ConnectException(secret) to "CONNECT_FAILURE", SSLHandshakeException(secret) to "TLS_FAILURE",
            HttpTimeoutException(secret) to "HTTP_TIMEOUT", TimeoutException(secret) to "HTTP_TIMEOUT",
            java.net.SocketTimeoutException(secret) to "HTTP_TIMEOUT", IOException(secret) to "IO_FAILURE",
            InterruptedException(secret) to "INTERRUPTED", IllegalStateException(secret) to "UNKNOWN_TRANSPORT_FAILURE",
        )
        causes.forEach { (cause, category) ->
            val wrapper = ExecutionException(java.util.concurrent.CompletionException(secret, cause)).also { it.addSuppressed(IOException(secret)) }
            val failure = adapter.incompleteResponse(null, cause = wrapper)
            assertThat(failure.detail).contains("($category)", "No upstream response headers received", "No automatic retry")
            assertThat(failure.detail).doesNotContain(secret, "Bearer", "private", "transcript")
            assertThat(failure.detail.length).isLessThanOrEqualTo(500)
            assertThat(failure.providerRequestId).isNull()
            assertThat(failure.cause).isNull()
        }
        val cycle = RuntimeException(secret)
        cycle.initCause(RuntimeException(secret, cycle))
        assertThat(adapter.transportCategory(cycle)).isEqualTo("UNKNOWN_TRANSPORT_FAILURE")
        assertThat(adapter.transportCategory(IOException(secret, UnknownHostException(secret)))).isEqualTo("DNS_FAILURE")
    }

    @Test fun `received success headers remain distinct from pre response failures`() {
        val info = object : HttpResponse.ResponseInfo {
            override fun statusCode() = 200
            override fun headers(): HttpHeaders = HttpHeaders.of(mapOf("x-request-id" to listOf("req_body_failure"))) { _, _ -> true }
            override fun version() = HttpClient.Version.HTTP_1_1
        }
        val failure = adapter.incompleteResponse(info, cause = IOException("private response"))
        assertThat(failure.detail).contains("IO_FAILURE", "Upstream HTTP 200", "response body unavailable")
        assertThat(failure.detail).doesNotContain("No upstream response", "private response")
        assertThat(failure.providerRequestId).isEqualTo("req_body_failure")
    }

    @Test fun `synchronous and asynchronous dispatch failures are classified without retry`() {
        listOf(false, true).forEach { synchronous ->
            val client = mock(HttpClient::class.java)
            `when`(client.followRedirects()).thenReturn(HttpClient.Redirect.NEVER)
            val cause = SSLHandshakeException("Bearer test-key private certificate path")
            if (synchronous) doAnswer { throw cause }.`when`(client).sendAsync<ByteArray>(any(), any())
            else doReturn(CompletableFuture.failedFuture<HttpResponse<ByteArray>>(cause)).`when`(client).sendAsync<ByteArray>(any(), any())
            val local = OpenAiAudioTranscriptionAdapter.forLocalCapture("test-key", URI("http://127.0.0.1:1234/v1/audio/transcriptions"), client)
            assertThatThrownBy { local.transcribe(AudioInput(wav(), 1000)) }.isInstanceOfSatisfying(AnalysisFailure::class.java) {
                assertThat(it.code).isEqualTo("TRANSCRIPTION_UNAVAILABLE")
                assertThat(it.detail).contains("TLS_FAILURE").doesNotContain("Bearer", "test-key", "certificate path")
                assertThat(it.cause).isNull()
            }
            verify(client, times(1)).sendAsync<ByteArray>(any(), any())
        }
    }
}
