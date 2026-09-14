package dev.sceneproof

import dev.sceneproof.film.AudioInput
import dev.sceneproof.film.OpenAiAudioTranscriptionAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpResponse
import java.time.Duration

class AudioTranscriptionDiagnosticsTest {
    private val adapter = OpenAiAudioTranscriptionAdapter("test-server-key")

    @ParameterizedTest
    @CsvSource("400,invalid_request_error,invalid_value", "401,invalid_request_error,invalid_api_key", "403,permission_error,permission_denied", "429,insufficient_quota,insufficient_quota", "429,requests,rate_limit_exceeded", "500,server_error,internal_error", "503,server_error,service_unavailable")
    fun `non success diagnostics retain real status and safe identifiers`(status: Int, type: String, code: String) {
        val failure = adapter.rejection(status, """{"error":{"type":"$type","code":"$code","message":"Untrusted request text"}}""".toByteArray(), "req_test_123")
        assertThat(failure.code).isEqualTo("TRANSCRIPTION_REJECTED")
        assertThat(failure.httpStatus).isEqualTo(502)
        assertThat(failure.providerRequestId).isEqualTo("req_test_123")
        assertThat(failure.detail).contains("HTTP $status", "type=$type", "code=$code", "Provider message withheld", "No automatic retry")
        assertThat(failure.detail).doesNotContain("Untrusted request text")
        assertThat(failure.detail.length).isLessThanOrEqualTo(500)
        assertThat(failure.cause).isNull()
    }

    @Test fun `only exact safe provider messages are retained never arbitrary prefixes`() {
        val safe = "You exceeded your current quota, please check your plan and billing details."
        val failure = adapter.rejection(429, """{"error":{"message":"$safe","code":"insufficient_quota","type":"insufficient_quota"}}""".toByteArray(), "req_test")
        assertThat(failure.detail).contains(safe)
        val unsafe = adapter.rejection(429, """{"error":{"message":"$safe Original private transcript"}}""".toByteArray(), "req_test")
        assertThat(unsafe.detail).doesNotContain(safe, "Original private transcript")
    }

    @Test fun `secrets transcript raw audio and arbitrary diagnostic fields are withheld`() {
        val body = """{"error":{"type":"private_transcript","code":"test-server-key","message":"Incorrect API key: sk-secret; Bearer test-server-key; private transcript; data:audio/wav;base64,PRIVATE","param":"private transcript"},"audio":"PRIVATE"}"""
        val failure = adapter.rejection(401, body.toByteArray(), "req_test-server-key")
        assertThat(failure.detail).contains("type=unavailable", "code=unavailable", "message withheld")
        assertThat(failure.detail).doesNotContain("private_transcript", "test-server-key", "sk-secret", "Bearer", "private transcript", "PRIVATE", "data:audio")
        assertThat(failure.providerRequestId).isNull()
        listOf("sk-secret", "req_sk-secret", "req_bad\r\nAuthorization: secret", "req_" + "a".repeat(157)).forEach { header ->
            assertThat(adapter.rejection(403, ByteArray(0), header).providerRequestId).isNull()
        }
    }

    @Test fun `malformed duplicate trailing oversized and non JSON bodies preserve status without body data`() {
        val bodies = listOf("<html>private proxy response</html>", "", "[]", "null", """{"error":"private response"}""", """{"error":{"message":"Invalid file format."},"error":{"message":"private"}}""", """{"error":{"message":"Invalid file format."}} {}""", """{"error":{"message":"Invalid file format."},"padding":"${"x".repeat(8192)}"}""")
        bodies.forEach { body ->
            val failure = adapter.rejection(400, body.toByteArray(), "req_bad_body")
            assertThat(failure.detail).contains("HTTP 400", "type=unavailable", "code=unavailable", "Provider message withheld")
            assertThat(failure.detail).doesNotContain("private", "Invalid file format.", "xxxx")
            assertThat(failure.providerRequestId).isEqualTo("req_bad_body")
            assertThat(failure.cause).isNull()
        }
    }

    @Test fun `received rejection headers survive body timeout interruption and size failure`() {
        val info = object : HttpResponse.ResponseInfo {
            override fun statusCode() = 503
            override fun headers(): HttpHeaders = HttpHeaders.of(mapOf("x-request-id" to listOf("req_headers"))) { _, _ -> true }
            override fun version(): HttpClient.Version = HttpClient.Version.HTTP_2
        }
        listOf(false, true).forEach { interrupted ->
            val failure = adapter.incompleteResponse(info, interrupted)
            assertThat(failure.code).isEqualTo("TRANSCRIPTION_REJECTED")
            assertThat(failure.detail).contains("HTTP 503", "Provider message withheld")
            assertThat(failure.providerRequestId).isEqualTo("req_headers")
        }
        assertThat(adapter.incompleteResponse(null).code).isEqualTo("TRANSCRIPTION_UNAVAILABLE")
        assertThat(adapter.incompleteResponse(null, true).code).isEqualTo("TRANSCRIPTION_INTERRUPTED")
    }

    @Test fun `request headers and complete binary multipart contract remain unchanged`() {
        val bytes = ByteArray(64) { it.toByte() }
        val input = AudioInput(bytes, 1000)
        val request = adapter.request(input, "test-boundary")
        assertThat(request.uri().toString()).isEqualTo("https://api.openai.com/v1/audio/transcriptions")
        assertThat(request.method()).isEqualTo("POST")
        assertThat(request.timeout().orElseThrow()).isEqualTo(Duration.ofSeconds(120))
        assertThat(request.headers().firstValue("Authorization").orElseThrow()).isEqualTo("Bearer test-server-key")
        assertThat(request.headers().firstValue("Content-Type").orElseThrow()).isEqualTo("multipart/form-data; boundary=test-boundary")
        assertThat(request.headers().firstValue("OpenAI-Project")).isEmpty()
        assertThat(request.headers().firstValue("OpenAI-Organization")).isEmpty()
        val expected = ("--test-boundary\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\nwhisper-1\r\n" +
            "--test-boundary\r\nContent-Disposition: form-data; name=\"response_format\"\r\n\r\nverbose_json\r\n" +
            "--test-boundary\r\nContent-Disposition: form-data; name=\"timestamp_granularities[]\"\r\n\r\nsegment\r\n" +
            "--test-boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"source.wav\"\r\nContent-Type: audio/wav\r\n\r\n").toByteArray() + bytes + "\r\n--test-boundary--\r\n".toByteArray()
        assertThat(adapter.requestBody(input, "test-boundary")).isEqualTo(expected)
        assertThat(request.bodyPublisher().orElseThrow().contentLength()).isEqualTo(expected.size.toLong())
        assertThat(expected.size).isLessThan(25_000_000)
    }
}
