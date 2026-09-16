package dev.sceneproof.demo

import dev.sceneproof.media.MediaFailure
import dev.sceneproof.media.MediaService
import dev.sceneproof.media.MediaStorage
import dev.sceneproof.project.ProjectRepository
import dev.sceneproof.project.ProjectService
import dev.sceneproof.project.ProjectView
import dev.sceneproof.reference.ReferenceService
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Service
class DemoService(
    private val source: DemoTemplateSource, private val projects: ProjectService,
    private val repository: ProjectRepository, private val references: ReferenceService,
    private val media: MediaService, private val storage: MediaStorage,
    private val films: dev.sceneproof.film.SourceFilmService,
    private val jdbc: JdbcTemplate, transactionManager: PlatformTransactionManager,
) {
    private val transaction = TransactionTemplate(transactionManager).apply { timeout = 180 }
    private val log = LoggerFactory.getLogger(DemoService::class.java)

    fun open(instanceId: UUID): ProjectView = populate(instanceId, null)

    fun reset(projectId: UUID): ProjectView {
        val project = projects.get(projectId)
        val identity = project.demo ?: throw MediaFailure("NOT_A_DEMO", "Only a demo copy can be reset.", 409)
        return populate(identity.instanceId, projectId)
    }

    private fun populate(instanceId: UUID, resetProjectId: UUID?): ProjectView {
        val createdAssets = mutableListOf<Pair<UUID, UUID>>()
        try {
            return requireNotNull(transaction.execute {
                jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?, 7))", instanceId.toString())
                val current = repository.findByDemoInstanceIdAndDemoRetiredFalse(instanceId)
                if (resetProjectId == null && current != null) return@execute ProjectView.from(current)
                if (resetProjectId != null) {
                    val replay = jdbc.queryForList("SELECT replacement_project_id FROM demo_replacements WHERE source_project_id = ?", resetProjectId)
                    if (replay.isNotEmpty()) return@execute ProjectView.from(requireNotNull(current))
                    if (current?.id != resetProjectId) throw MediaFailure("DEMO_CHANGED", "This demo copy has already been replaced. Open the current demo from the landing page.", 409)
                    jdbc.execute("SELECT pg_advisory_xact_lock(731204)")
                    val active = jdbc.queryForObject("SELECT (SELECT count(*) FROM film_understanding_runs WHERE project_id = ? AND completed_at IS NULL) + (SELECT count(*) FROM analysis_runs WHERE project_id = ? AND status = 'RUNNING')", Long::class.java, resetProjectId, resetProjectId)!!
                    if (active > 0) throw MediaFailure("DEMO_BUSY", "Wait for this demo's active analysis to finish before resetting.", 409)
                }
                val template = source.template()
                if (current != null && current.demoTemplateVersion != template.version) {
                    throw MediaFailure("DEMO_VERSION_UNAVAILABLE", "This copy needs its original template version to reset. Ask the operator to restore that version.", 409)
                }
                if (current != null) {
                    current.demoRetired = true
                    repository.saveAndFlush(current)
                }
                val project = projects.createDemo(template.project, instanceId, template.version)
                template.sourceFilm?.let { asset ->
                    val film = films.upload(project.id, source.upload(asset))
                    createdAssets.add(project.id to film.id)
                }
                template.references.forEach { asset ->
                    val reference = references.create(project.id, source.upload(asset), asset.title, asset.guidance)
                    createdAssets.add(project.id to reference.id)
                }
                template.shots.forEach { asset ->
                    val shot = media.ingest(project.id, source.upload(asset))
                    check(shot.status == "READY" && shot.frames.isNotEmpty())
                    createdAssets.add(project.id to shot.id)
                }
                if (resetProjectId != null) jdbc.update("INSERT INTO demo_replacements (source_project_id, replacement_project_id) VALUES (?, ?)", resetProjectId, project.id)
                project
            })
        } catch (failure: Exception) {
            createdAssets.forEach { (projectId, assetId) ->
                try {
                    val persisted = jdbc.queryForObject("SELECT count(*) FROM projects WHERE id = ?", Long::class.java, projectId)
                    if (persisted == 0L) storage.delete(projectId, assetId)
                } catch (_: Exception) { log.warn("Demo asset cleanup deferred for {}", assetId) }
            }
            throw failure as? MediaFailure ?: MediaFailure("DEMO_PREPARATION_FAILED", "The demo could not be prepared. Check its installed assets and retry the same request.", 503)
        }
    }
}
