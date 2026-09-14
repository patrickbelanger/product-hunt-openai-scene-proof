package dev.sceneproof.project

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

class ProjectNotFound : RuntimeException()

@Service
@Transactional(readOnly = true)
class ProjectService(private val repository: ProjectRepository) {
    fun list(page: Int): ProjectPage {
        val projects = repository.findByDemoRetiredFalse(
            PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id")),
        )
        return ProjectPage(projects.content.map(ProjectView::from), page, projects.hasNext())
    }

    fun get(id: UUID): ProjectView = ProjectView.from(
        repository.findById(id).orElseThrow { ProjectNotFound() },
    )

    @Transactional
    fun updateRules(id: UUID, request: UpdateRulesRequest): ProjectView {
        val project = repository.findById(id).orElseThrow { ProjectNotFound() }
        project.rules = request.rules.trim()
        project.updatedAt = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS)
        return ProjectView.from(repository.save(project))
    }

    @Transactional
    fun createDemo(request: CreateProjectRequest, instanceId: UUID, version: String): ProjectView = ProjectView.from(
        repository.saveAndFlush(Project(name = request.name.trim(), description = request.description.trim(),
            rules = request.rules.trim(), demoInstanceId = instanceId, demoTemplateVersion = version)),
    )

    @Transactional
    fun create(request: CreateProjectRequest): ProjectView = ProjectView.from(
        repository.save(
            Project(
                name = request.name.trim(),
                description = request.description.trim(),
                rules = request.rules.trim(),
            ),
        ),
    )
}
