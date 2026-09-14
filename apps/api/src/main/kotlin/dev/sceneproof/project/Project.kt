package dev.sceneproof.project

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "projects")
class Project(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 120)
    val name: String,
    @Column(nullable = false, length = 2000)
    val description: String = "",
    @Column(nullable = false, length = 8000)
    var rules: String = "",
    @Column(nullable = false)
    val createdAt: Instant = Instant.now().truncatedTo(ChronoUnit.MICROS),
    @Column(nullable = false)
    var updatedAt: Instant = createdAt,
    val demoInstanceId: UUID? = null,
    @Column(length = 80)
    val demoTemplateVersion: String? = null,
    var demoRetired: Boolean = false,
)
