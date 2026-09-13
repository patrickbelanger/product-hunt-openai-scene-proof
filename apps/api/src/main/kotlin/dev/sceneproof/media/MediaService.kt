package dev.sceneproof.media

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.util.UUID

@Service
class MediaService(private val repository: MediaRepository, private val storage: MediaStorage, private val images: ImageNormalizer, private val video: FfmpegAdapter, private val gate: MediaIngestionGate) {
    private val log = LoggerFactory.getLogger(MediaService::class.java)

    fun ingest(projectId: UUID, upload: MultipartFile): ShotView {
        repository.checkCapacity(projectId)
        gate.acquire()
        val shotId = UUID.randomUUID()
        val name = upload.originalFilename.orEmpty().substringAfterLast('/').substringAfterLast('\\').filter { !it.isISOControl() }.take(160).ifBlank { "Untitled shot" }
        try {
            val input = storage.file(projectId, shotId, "original.bin")
            ImageContent.copy(upload, input, 100L * 1024 * 1024)
            val signature = Files.newInputStream(input).use { it.readNBytes(12) }
            val media = when {
                ImageContent.hasImageSignature(signature) -> {
                    if (Files.size(input) > 10L * 1024 * 1024) throw MediaFailure("IMAGE_TOO_LARGE", "Images must be at most 10 MiB.", 413)
                    PreparedMedia("IMAGE", null, listOf(images.normalize(input, storage.file(projectId, shotId, "00.png"))))
                }
                signature.size == 12 && String(signature, 4, 4, Charsets.US_ASCII) == "ftyp" -> video.extract(input)
                else -> throw MediaFailure("UNSUPPORTED_MEDIA", "Use a valid JPEG, PNG or MP4/H.264 file.", 415)
            }
            return repository.save(projectId, shotId, name, media, null)
        } catch (exception: Exception) {
            val failure = exception as? MediaFailure ?: MediaFailure("STORAGE_FAILURE", "Media could not be stored. Please retry.", 503)
            try { storage.delete(projectId, shotId) } catch (_: Exception) { log.warn("Media cleanup failed for shot {}", shotId) }
            try { repository.save(projectId, shotId, name, null, failure.code) } catch (_: Exception) { log.warn("Failed import could not be recorded for shot {}", shotId) }
            throw failure
        } finally { gate.release() }
    }
}
