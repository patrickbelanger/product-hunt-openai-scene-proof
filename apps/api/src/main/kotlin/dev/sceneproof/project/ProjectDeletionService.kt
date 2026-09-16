package dev.sceneproof.project

import dev.sceneproof.analysis.AnalysisFailure
import dev.sceneproof.media.MediaIngestionGate
import dev.sceneproof.media.MediaStorage
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import java.util.UUID

@Service
class ProjectDeletionService(private val jdbc: JdbcTemplate, transactionManager: PlatformTransactionManager, private val storage: MediaStorage, private val gate: MediaIngestionGate, private val work: dev.sceneproof.analysis.PaidWorkGate) {
    private val transaction = TransactionTemplate(transactionManager).apply { propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW; timeout = 30 }
    fun delete(id: UUID, confirmationName: String) {
        try { work.acquire() } catch (_: AnalysisFailure) {
            throw AnalysisFailure("PROJECT_BUSY", "Wait for active analysis to finish before deleting this project.", 409)
        }
        var acquired = false
        try {
            gate.acquire()
            acquired = true
            transaction.executeWithoutResult {
                jdbc.execute("SELECT pg_advisory_xact_lock(731204)")
                val project = jdbc.query("SELECT name, demo_instance_id FROM projects WHERE id = ? FOR UPDATE", { row, _ -> row.getString("name") to row.getObject("demo_instance_id") }, id).firstOrNull()
                if (project == null) {
                    if (jdbc.queryForObject("SELECT count(*) FROM project_deletions WHERE project_id = ?", Long::class.java, id) == 0L) throw ProjectNotFound()
                } else {
                    if (project.second != null) throw AnalysisFailure("DEMO_DELETE_DISABLED", "Demo copies retain their history. Use Reset demo on the current copy.", 409)
                    if (project.first != confirmationName) throw AnalysisFailure("DELETE_CONFIRMATION_REQUIRED", "Type the project name exactly to confirm irreversible deletion.", 400)
                    val active = jdbc.queryForObject("SELECT (SELECT count(*) FROM film_understanding_runs WHERE project_id = ? AND completed_at IS NULL) + (SELECT count(*) FROM analysis_runs WHERE project_id = ? AND status = 'RUNNING')", Long::class.java, id, id)!!
                    if (active > 0) throw AnalysisFailure("PROJECT_BUSY", "Wait for active analysis to finish before deleting this project.", 409)
                    jdbc.update("INSERT INTO project_deletions (project_id) VALUES (?)", id)
                    jdbc.update("DELETE FROM projects WHERE id = ?", id)
                }
            }
            try {
                storage.deleteProject(id)
                jdbc.update("UPDATE project_deletions SET media_cleaned_at = clock_timestamp() WHERE project_id = ?", id)
            } catch (_: Exception) {
                throw AnalysisFailure("PROJECT_CLEANUP_PENDING", "Project records were deleted, but media cleanup is pending. Retry deletion to finish cleanup safely.", 503)
            }
        } finally { if (acquired) gate.release(); work.release() }
    }
}
