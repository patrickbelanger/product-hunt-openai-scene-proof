package dev.sceneproof.reference

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class ReferenceMetadataRequest(@field:NotBlank @field:Size(max = 120) val title: String, @field:Size(max = 2000) val guidance: String)

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
class ReferenceController(private val repository: ReferenceRepository, private val service: ReferenceService) {
    @GetMapping("/references")
    fun list(@PathVariable projectId: UUID): List<ReferenceView> = repository.list(projectId)

    @GetMapping("/references/{referenceId}")
    fun get(@PathVariable projectId: UUID, @PathVariable referenceId: UUID): ReferenceView = repository.get(projectId, referenceId)

    @PostMapping("/references", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun create(@PathVariable projectId: UUID, @RequestPart("file") file: MultipartFile, @RequestParam title: String, @RequestParam guidance: String): ResponseEntity<ReferenceView> =
        ResponseEntity.status(201).body(service.create(projectId, file, title, guidance))

    @PutMapping("/references/{referenceId}")
    fun update(@PathVariable projectId: UUID, @PathVariable referenceId: UUID, @Valid @RequestBody request: ReferenceMetadataRequest): ReferenceView = repository.update(projectId, referenceId, request)

    @PostMapping("/references/{referenceId}/archive")
    fun archive(@PathVariable projectId: UUID, @PathVariable referenceId: UUID): ReferenceView = repository.archive(projectId, referenceId)

    @GetMapping("/references/{referenceId}/content")
    fun content(@PathVariable projectId: UUID, @PathVariable referenceId: UUID): ResponseEntity<ByteArray> =
        ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options", "nosniff")
            .header("Cache-Control", "private, no-cache").body(service.content(repository.get(projectId, referenceId)))

    @GetMapping("/findings/{findingId}/references")
    fun findingReferences(@PathVariable projectId: UUID, @PathVariable findingId: UUID): List<ReferenceView> = repository.findingReferences(projectId, findingId)
}
