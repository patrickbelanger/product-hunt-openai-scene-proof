package dev.sceneproof.analysis

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.UUID

data class CreateFindingActionRequest(
    val requestId: UUID,
    val type: FindingActionType,
    @field:NotBlank @field:Size(max = 2000) val explanation: String,
    @field:NotBlank @field:Size(max = 1000) val scope: String,
    @field:Size(min = 1, max = 8) val affectedShotIds: List<UUID>,
)

@RestController
@RequestMapping("/api/v1/projects/{projectId}/findings/{findingId}")
class FindingActionController(private val service: FindingActionService, private val actions: FindingActionRepository) {
    @PostMapping("/actions")
    fun act(@PathVariable projectId: UUID, @PathVariable findingId: UUID, @Valid @RequestBody request: CreateFindingActionRequest): ResponseEntity<FindingActionView> {
        val action = service.act(projectId, findingId, request)
        return ResponseEntity.ok().location(URI.create("/api/v1/projects/$projectId/findings/$findingId/actions/${action.id}")).body(action)
    }

    @GetMapping("/actions")
    fun history(@PathVariable projectId: UUID, @PathVariable findingId: UUID) = actions.history(projectId, findingId)

    @GetMapping("/actions/{actionId}")
    fun get(@PathVariable projectId: UUID, @PathVariable findingId: UUID, @PathVariable actionId: UUID) = actions.get(projectId, findingId, actionId)
}
