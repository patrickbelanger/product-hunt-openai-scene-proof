package dev.sceneproof.reference

import dev.sceneproof.media.ImageContent
import dev.sceneproof.media.ImageNormalizer
import dev.sceneproof.media.MediaFailure
import dev.sceneproof.media.MediaIngestionGate
import dev.sceneproof.media.MediaStorage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.util.UUID

@Service
class ReferenceService(private val repository: ReferenceRepository, private val storage: MediaStorage, private val images: ImageNormalizer, private val gate: MediaIngestionGate) {
    private val log = LoggerFactory.getLogger(ReferenceService::class.java)

    fun create(projectId: UUID, upload: MultipartFile, title: String, guidance: String): ReferenceView {
        if (title.isBlank() || title.length > 120 || guidance.length > 2000) throw MediaFailure("INVALID_REFERENCE_METADATA", "Supply a title up to 120 characters and guidance up to 2000 characters.", 400)
        repository.checkCapacity(projectId)
        gate.acquire()
        val id = UUID.randomUUID()
        try {
            val input = storage.file(projectId, id, "original.bin")
            ImageContent.copy(upload, input, 10L * 1024 * 1024)
            val signature = Files.newInputStream(input).use { it.readNBytes(12) }
            if (!ImageContent.hasImageSignature(signature)) throw MediaFailure("UNSUPPORTED_REFERENCE", "Use a valid JPEG or PNG reference image.", 415)
            val output = storage.file(projectId, id, "00.png")
            val image = images.normalize(input, output)
            val hash = ImageContent.sha256(ImageContent.readNormalized(output, image.width, image.height))
            return repository.create(projectId, id, title, guidance, image, hash)
        } catch (exception: Exception) {
            try { storage.delete(projectId, id) } catch (_: Exception) { log.warn("Reference cleanup failed for {}", id) }
            throw exception as? MediaFailure ?: MediaFailure("REFERENCE_STORAGE_FAILURE", "The reference could not be stored. Please retry.", 503)
        } finally { gate.release() }
    }

    fun content(reference: ReferenceView): ByteArray = try {
        ImageContent.readNormalized(storage.file(reference.projectId, reference.id, "00.png"), reference.width, reference.height, reference.sha256)
    } catch (_: Exception) {
        throw MediaFailure("REFERENCE_UNAVAILABLE", "The stored reference image is missing or unusable.", 503)
    }
}
