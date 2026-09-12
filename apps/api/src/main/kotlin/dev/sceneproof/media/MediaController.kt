package dev.sceneproof.media

import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
class MediaController(private val service: MediaService, private val repository: MediaRepository, private val storage: MediaStorage) {
    @GetMapping("/shots")
    fun list(@PathVariable projectId: UUID): List<ShotView> = repository.list(projectId)

    @PostMapping("/shots", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@PathVariable projectId: UUID, @RequestPart("file") file: MultipartFile): ResponseEntity<ShotView> = ResponseEntity.status(201).body(service.ingest(projectId, file))

    @GetMapping("/frames/{frameId}/content")
    fun content(@PathVariable projectId: UUID, @PathVariable frameId: UUID): ResponseEntity<FileSystemResource> {
        val (shotId, key) = repository.content(projectId, frameId)
        val resource = FileSystemResource(storage.file(projectId, shotId, key))
        if (!resource.isReadable) throw MediaFailure("MEDIA_UNAVAILABLE", "The stored frame is unavailable.", 503)
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options", "nosniff")
            .header("Cache-Control", "private, no-cache").contentLength(resource.contentLength()).body(resource)
    }
}
