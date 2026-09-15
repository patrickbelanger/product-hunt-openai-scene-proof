package dev.sceneproof.api

import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.Semaphore

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class HttpSafetyFilter(private val storage: dev.sceneproof.media.MediaStorage, private val admission: RequestAdmission) : OncePerRequestFilter() {
    private val uploads = Semaphore(1)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        response.setHeader("X-Content-Type-Options", "nosniff")
        response.setHeader("X-Frame-Options", "DENY")
        response.setHeader("Referrer-Policy", "no-referrer")
        response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'")
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
        response.setHeader("Cache-Control", "no-store")
        if (!request.requestURI.startsWith("/api/")) {
            chain.doFilter(request, response)
            return
        }
        val mutation = request.method !in setOf("GET", "HEAD", "OPTIONS")
        val origin = request.getHeader("Origin")
        val expectedOrigin = "${request.scheme}://${request.serverName}${if (request.serverPort == 80 && request.scheme == "http" || request.serverPort == 443 && request.scheme == "https") "" else ":${request.serverPort}"}"
        if (mutation && (request.getHeader("Sec-Fetch-Site") in setOf("cross-site", "same-site") || origin != null && origin != expectedOrigin)) {
            reject(response, 403, "cross-origin-rejected", "Use the same-origin SceneProof application for changes.")
            return
        }
        val multipart = request.contentType?.startsWith("multipart/", ignoreCase = true) == true
        val expensive = multipart || (mutation && (request.requestURI == "/api/v1/demo" || request.requestURI.endsWith("/demo/reset")))
        if (!admission.accept(mutation, expensive)) {
            reject(response, 429, "request-rate-limit", "The deployment's request safety limit has been reached. Try later.")
            return
        }
        if (expensive && !uploads.tryAcquire()) {
            reject(response, 429, "ingestion-busy", "Another upload or demo preparation is processing. Try later.")
            return
        }
        try {
            if (expensive) {
                try { storage.checkWriteCapacity() }
                catch (failure: dev.sceneproof.media.MediaFailure) {
                    reject(response, failure.httpStatus, "storage-capacity", failure.detail)
                    return
                }
            }
            if (mutation && !multipart) {
                val bytes = request.inputStream.readNBytes(65_537)
                if (bytes.size > 65_536) {
                    reject(response, 413, "request-too-large", "JSON requests must be at most 64 KiB.")
                    return
                }
                val bounded = object : HttpServletRequestWrapper(request) {
                    override fun getInputStream(): ServletInputStream {
                        val input = bytes.inputStream()
                        return object : ServletInputStream() {
                            override fun read(): Int = input.read()
                            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = input.read(buffer, offset, length)
                            override fun isFinished(): Boolean = input.available() == 0
                            override fun isReady(): Boolean = true
                            override fun setReadListener(listener: ReadListener) { throw UnsupportedOperationException() }
                        }
                    }
                    override fun getReader() = getInputStream().bufferedReader(Charsets.UTF_8)
                }
                chain.doFilter(bounded, response)
            } else chain.doFilter(request, response)
        } finally { if (expensive) uploads.release() }
    }

    private fun reject(response: HttpServletResponse, status: Int, code: String, detail: String) {
        response.status = status
        response.contentType = "application/problem+json"
        response.writer.write("""{"type":"urn:sceneproof:problem:$code","status":$status,"detail":"$detail"}""")
    }
}
