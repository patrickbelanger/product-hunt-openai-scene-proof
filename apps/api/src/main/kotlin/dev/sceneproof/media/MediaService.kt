package dev.sceneproof.media

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.util.UUID
import java.util.concurrent.Semaphore

@Service
class MediaService(private val repository: MediaRepository, private val storage: MediaStorage, private val images: ImageNormalizer, private val video: FfmpegAdapter) {
    private val permits = Semaphore(1)
    private val log = LoggerFactory.getLogger(MediaService::class.java)

    fun ingest(projectId: UUID, upload: MultipartFile): ShotView {
        repository.checkCapacity(projectId)
        if (!permits.tryAcquire()) throw MediaFailure("INGESTION_BUSY", "Another import is processing. Please retry shortly.", 429)
        val shotId = UUID.randomUUID()
        val name = upload.originalFilename.orEmpty().substringAfterLast('/').substringAfterLast('\\').filter { !it.isISOControl() }.take(160).ifBlank { "Untitled shot" }
        try {
            val input = storage.file(projectId, shotId, "original.bin")
            upload.inputStream.use { source ->
                Files.newOutputStream(input, CREATE_NEW).use { destination ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > 100L * 1024 * 1024) throw MediaFailure("UPLOAD_TOO_LARGE", "Files must be at most 100 MiB.", 413)
                        destination.write(buffer, 0, count)
                    }
                }
            }
            val signature = Files.newInputStream(input).use { it.readNBytes(12) }
            val png = signature.take(8) == listOf(137, 80, 78, 71, 13, 10, 26, 10).map { it.toByte() }
            val jpeg = signature.size >= 3 && signature[0] == 0xff.toByte() && signature[1] == 0xd8.toByte() && signature[2] == 0xff.toByte()
            val media = when {
                png || jpeg -> {
                    if (Files.size(input) > 10L * 1024 * 1024) throw MediaFailure("IMAGE_TOO_LARGE", "Images must be at most 10 MiB.", 413)
                    PreparedMedia("IMAGE", null, listOf(images.normalize(input, storage.file(projectId, shotId, "00.png"))))
                }
                signature.size == 12 && String(signature, 4, 4, Charsets.US_ASCII) == "ftyp" && String(signature, 8, 4, Charsets.US_ASCII) in setOf("isom", "iso2", "mp41", "mp42", "avc1", "M4V ") -> video.extract(input)
                else -> throw MediaFailure("UNSUPPORTED_MEDIA", "Use a valid JPEG, PNG or MP4/H.264 file.", 415)
            }
            return repository.save(projectId, shotId, name, media, null)
        } catch (exception: Exception) {
            val failure = exception as? MediaFailure ?: MediaFailure("STORAGE_FAILURE", "Media could not be stored. Please retry.", 503)
            try { storage.delete(projectId, shotId) } catch (_: Exception) { log.warn("Media cleanup failed for shot {}", shotId) }
            try { repository.save(projectId, shotId, name, null, failure.code) } catch (_: Exception) { log.warn("Failed import could not be recorded for shot {}", shotId) }
            throw failure
        } finally { permits.release() }
    }
}
