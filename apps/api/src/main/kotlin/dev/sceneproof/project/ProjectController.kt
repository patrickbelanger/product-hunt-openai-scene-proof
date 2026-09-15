package dev.sceneproof.project

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Instant
import java.util.UUID

data class CreateProjectRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,
    @field:Size(max = 2000)
    val description: String = "",
    @field:Size(max = 8000)
    val rules: String = "",
)

data class ProjectView(
    val id: UUID,
    val name: String,
    val description: String,
    val rules: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val demo: DemoIdentity? = null,
) {
    companion object {
        fun from(project: Project) = ProjectView(
            project.id, project.name, project.description, project.rules,
            project.createdAt, project.updatedAt,
            project.demoInstanceId?.let { DemoIdentity(it, requireNotNull(project.demoTemplateVersion), project.demoRetired) },
        )
    }
}

data class DemoIdentity(val instanceId: UUID, val templateVersion: String, val retired: Boolean)

data class ProjectPage(val items: List<ProjectView>, val page: Int, val hasNext: Boolean)

data class UpdateRulesRequest(@field:Size(max = 8000) val rules: String)
data class DeleteProjectRequest(@field:NotBlank @field:Size(max = 120) val confirmationName: String)

@RestController
@RequestMapping("/api/v1/projects")
class ProjectController(private val service: ProjectService, private val deletion: ProjectDeletionService) {
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID, @Valid @RequestBody request: DeleteProjectRequest): ResponseEntity<Void> {
        deletion.delete(id, request.confirmationName)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun list(@RequestParam(defaultValue = "0") @Min(0) @Max(10000) page: Int): ProjectPage = service.list(page)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ProjectView = service.get(id)

    @PutMapping("/{id}/rules")
    fun updateRules(@PathVariable id: UUID, @Valid @RequestBody request: UpdateRulesRequest): ProjectView = service.updateRules(id, request)

    @PostMapping
    fun create(@Valid @RequestBody request: CreateProjectRequest): ResponseEntity<ProjectView> {
        val project = service.create(request)
        return ResponseEntity.created(URI.create("/api/v1/projects/${project.id}")).body(project)
    }
}
