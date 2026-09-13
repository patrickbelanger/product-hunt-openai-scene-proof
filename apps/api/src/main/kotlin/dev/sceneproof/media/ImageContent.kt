package dev.sceneproof.media

import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.security.MessageDigest
import java.util.HexFormat
import java.util.concurrent.Semaphore
import javax.imageio.ImageIO

@Component
class MediaIngestionGate {
    private val permits = Semaphore(1)
    fun acquire() {
        if (!permits.tryAcquire()) throw MediaFailure("INGESTION_BUSY", "Another import is processing. Please retry shortly.", 429)
    }
    fun release() = permits.release()
}

object ImageContent {
    fun copy(upload: MultipartFile, destination: Path, limit: Long) {
        upload.inputStream.use { source ->
            Files.newOutputStream(destination, CREATE_NEW).use { output ->
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > limit) throw MediaFailure("UPLOAD_TOO_LARGE", "The upload exceeds its size limit.", 413)
                    output.write(buffer, 0, count)
                }
            }
        }
    }

    fun hasImageSignature(signature: ByteArray): Boolean = isPng(signature) ||
        (signature.size >= 3 && signature[0] == 0xff.toByte() && signature[1] == 0xd8.toByte() && signature[2] == 0xff.toByte())

    private fun isPng(bytes: ByteArray) = bytes.take(8) == listOf(137, 80, 78, 71, 13, 10, 26, 10).map { it.toByte() }

    fun readNormalized(file: Path, width: Int, height: Int, expectedHash: String? = null): ByteArray {
        require(Files.isRegularFile(file) && Files.size(file) in 1..8L * 1024 * 1024)
        val png = Files.newInputStream(file).use { it.readNBytes(8 * 1024 * 1024 + 1) }
        require(png.size <= 8 * 1024 * 1024 && isPng(png))
        require(width in 1..1600 && height in 1..1600)
        if (expectedHash != null) require(sha256(png) == expectedHash)
        ImageIO.createImageInputStream(png.inputStream()).use { input ->
            val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: error("Invalid PNG")
            try {
                reader.input = input
                require(reader.getWidth(0) == width && reader.getHeight(0) == height)
                reader.addIIOReadWarningListener { _, _ -> error("Corrupt PNG") }
                val decoded = reader.read(0) ?: error("Invalid PNG")
                decoded.flush()
            } finally { reader.dispose() }
        }
        return png
    }

    fun sha256(bytes: ByteArray): String = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
}
