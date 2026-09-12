package dev.sceneproof.media

import org.springframework.stereotype.Component
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageInputStream
import kotlin.math.roundToInt

class MediaFailure(val code: String, val detail: String, val httpStatus: Int = 422) : RuntimeException()

data class PreparedFrame(val key: String, val timestampMs: Long?, val width: Int, val height: Int)
data class PreparedMedia(val kind: String, val durationMs: Long?, val frames: List<PreparedFrame>)

@Component
class ImageNormalizer {
    fun normalize(input: Path, output: Path, timestampMs: Long? = null): PreparedFrame {
        try {
            FileImageInputStream(input.toFile()).use { stream ->
                val readers = ImageIO.getImageReaders(stream)
                if (!readers.hasNext()) throw MediaFailure("INVALID_IMAGE", "Use a valid JPEG or PNG image.")
                val reader = readers.next()
                try {
                    if (reader.formatName.lowercase() !in setOf("jpeg", "png")) throw MediaFailure("INVALID_IMAGE", "Use a JPEG or PNG image.")
                    reader.input = stream
                    val width = reader.getWidth(0)
                    val height = reader.getHeight(0)
                    if (width !in 1..8192 || height !in 1..8192 || width.toLong() * height > 16_000_000) {
                        throw MediaFailure("IMAGE_DIMENSIONS", "Images must be at most 16 megapixels and 8192 pixels per side.")
                    }
                    reader.addIIOReadWarningListener { _, _ -> throw MediaFailure("INVALID_IMAGE", "The image is incomplete or corrupt.") }
                    val decoded = reader.read(0)
                    val scale = minOf(1.0, 1600.0 / maxOf(width, height))
                    val normalized = BufferedImage(maxOf(1, (width * scale).roundToInt()), maxOf(1, (height * scale).roundToInt()), BufferedImage.TYPE_INT_RGB)
                    val graphics = normalized.createGraphics()
                    try {
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                        graphics.drawImage(decoded, 0, 0, normalized.width, normalized.height, null)
                    } finally { graphics.dispose(); decoded.flush() }
                    Files.newOutputStream(output).use { if (!ImageIO.write(normalized, "png", it)) error("PNG writer unavailable") }
                    return PreparedFrame(output.fileName.toString(), timestampMs, normalized.width, normalized.height)
                } finally { reader.dispose() }
            }
        } catch (failure: MediaFailure) { throw failure }
        catch (_: Exception) { throw MediaFailure("INVALID_IMAGE", "The image could not be decoded or stored.") }
    }
}
