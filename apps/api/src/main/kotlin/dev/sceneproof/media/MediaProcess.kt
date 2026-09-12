package dev.sceneproof.media

import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Component
class MediaProcess {
    fun run(arguments: List<String>, timeoutSeconds: Long = 60): String {
        val process = try { ProcessBuilder(arguments).redirectErrorStream(true).start() }
        catch (_: Exception) { throw MediaFailure("DECODER_UNAVAILABLE", "FFmpeg and FFprobe must be installed on the server.", 503) }
        val output = ByteArrayOutputStream()
        val overflow = AtomicBoolean(false)
        val reader = Thread.ofVirtual().start {
            try {
                process.inputStream.use { stream ->
                    val buffer = ByteArray(4096)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        if (output.size() + count > 262144) { overflow.set(true); process.destroyForcibly(); break }
                        output.write(buffer, 0, count)
                    }
                }
            } catch (_: Exception) { overflow.set(true) }
        }
        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) throw MediaFailure("DECODER_TIMEOUT", "Media processing exceeded its time limit. Try a shorter or smaller file.")
            reader.join(5000)
            if (reader.isAlive || overflow.get() || process.exitValue() != 0) throw MediaFailure("INVALID_VIDEO", "The video could not be decoded within the processing limits.")
            return output.toString(Charsets.UTF_8)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            throw MediaFailure("DECODER_INTERRUPTED", "Media processing was interrupted.", 503)
        } finally {
            process.descendants().forEach { it.destroyForcibly() }
            if (process.isAlive) process.destroyForcibly()
            process.waitFor(5, TimeUnit.SECONDS)
            reader.join(5000)
        }
    }
}
