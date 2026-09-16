package dev.sceneproof

import dev.sceneproof.api.HttpSafetyFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class HttpSafetyFilterTest {
    private fun filter() = HttpSafetyFilter(org.mockito.Mockito.mock(dev.sceneproof.media.MediaStorage::class.java), dev.sceneproof.api.RequestAdmission(600, 120, 30))

    @Test fun `global rolling request buckets bound memory and do not depend on forwarded addresses`() {
        val admission = dev.sceneproof.api.RequestAdmission(2, 1, 1)
        assertThat(admission.accept(false, false, 0)).isTrue()
        assertThat(admission.accept(false, false, 1)).isTrue()
        assertThat(admission.accept(false, false, 59_999_999_999)).isFalse()
        assertThat(admission.accept(true, false, 1)).isTrue()
        assertThat(admission.accept(true, false, 2)).isFalse()
        assertThat(admission.accept(true, true, 1)).isTrue()
        assertThat(admission.accept(true, true, 2)).isFalse()
        assertThat(admission.accept(false, false, 60_000_000_000)).isTrue()
    }

    @Test fun `storage pressure rejects before multipart parsing without exposing internals`() {
        val storage = org.mockito.Mockito.mock(dev.sceneproof.media.MediaStorage::class.java)
        org.mockito.Mockito.doThrow(dev.sceneproof.media.MediaFailure("STORAGE_CAPACITY", "Storage safety reserve reached.", 503)).`when`(storage).checkWriteCapacity()
        val filter = HttpSafetyFilter(storage, dev.sceneproof.api.RequestAdmission(600, 120, 30))
        val request = MockHttpServletRequest("POST", "/api/v1/projects/fixture/shots").apply { contentType = "multipart/form-data" }
        val response = MockHttpServletResponse()
        filter.doFilter(request, response) { _, _ -> error("Storage-pressure request dispatched") }
        assertThat(response.status).isEqualTo(503)
        assertThat(response.contentAsString).contains("storage-capacity").doesNotContain("java.", "C:\\")
    }
    @Test fun `security headers cover errors and private reads without enabling CORS`() {
        val request = MockHttpServletRequest("GET", "/api/v1/projects")
        val response = MockHttpServletResponse()
        filter().doFilter(request, response) { _, _ -> }
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store")
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff")
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY")
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer")
        assertThat(response.getHeader("Content-Security-Policy")).contains("frame-ancestors 'none'")
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull()
    }

    @Test fun `rejects oversized chunked JSON before MVC and preserves valid bodies`() {
        listOf(65_536, 65_537).forEach { size ->
            val request = MockHttpServletRequest("POST", "/api/v1/projects").apply { setContent(ByteArray(size) { 32 }) }
            val response = MockHttpServletResponse()
            var dispatched = false
            filter().doFilter(request, response) { supplied, _ ->
                dispatched = true
                assertThat(supplied.inputStream.readAllBytes()).hasSize(size)
            }
            assertThat(dispatched).isEqualTo(size == 65_536)
            assertThat(response.status).isEqualTo(if (size == 65_536) 200 else 413)
        }
    }

    @Test fun `rejects cross origin and null origin mutations but permits same origin and CLI`() {
        listOf("https://attacker.example", "null", "http://localhost", null).forEach { origin ->
            val request = MockHttpServletRequest("POST", "/api/v1/projects").apply { if (origin != null) addHeader("Origin", origin) }
            val response = MockHttpServletResponse()
            filter().doFilter(request, response) { _, _ -> }
            assertThat(response.status).isEqualTo(if (origin == null || origin == "http://localhost") 200 else 403)
        }
    }

    @Test fun `rejects concurrent multipart before reading its body and releases permit after failure`() {
        val filter = filter()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val worker = Thread.ofVirtual().start {
            filter.doFilter(MockHttpServletRequest("POST", "/api/v1/demo"), MockHttpServletResponse()) { _, _ ->
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
            }
        }
        try {
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue()
            val request = MockHttpServletRequest("POST", "/api/v1/projects/fixture/shots").apply { contentType = "multipart/form-data" }
            val response = MockHttpServletResponse()
            filter.doFilter(request, response) { _, _ -> error("Concurrent upload dispatched") }
            assertThat(response.status).isEqualTo(429)
        } finally { release.countDown(); worker.join(5000) }
        val response = MockHttpServletResponse()
        filter.doFilter(MockHttpServletRequest("POST", "/api/v1/demo"), response) { _, _ -> }
        assertThat(response.status).isEqualTo(200)
    }
}
