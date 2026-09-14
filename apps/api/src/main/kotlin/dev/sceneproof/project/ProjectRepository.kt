package dev.sceneproof.project

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface ProjectRepository : JpaRepository<Project, UUID> {
    fun findByDemoRetiredFalse(pageable: Pageable): Page<Project>
    fun findByDemoInstanceIdAndDemoRetiredFalse(instanceId: UUID): Project?
}
