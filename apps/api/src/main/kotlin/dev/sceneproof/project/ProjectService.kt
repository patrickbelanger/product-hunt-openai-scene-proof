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
        val projects = repository.findAll(
            PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id")),
        )
        return ProjectPage(projects.content.map(ProjectView::from), page, projects.hasNext())
    }

    fun get(id: UUID): ProjectView = ProjectView.from(
        repository.findById(id).orElseThrow { ProjectNotFound() },
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
