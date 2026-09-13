package dev.sceneproof.reference

import dev.sceneproof.media.MediaFailure
import dev.sceneproof.media.MediaRepository
import dev.sceneproof.media.PreparedFrame
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class ReferenceView(
    val id: UUID, val projectId: UUID, val title: String, val guidance: String,
    val width: Int, val height: Int, val sha256: String, val createdAt: Instant,
    val archivedAt: Instant?, val url: String,
)

@Repository
class ReferenceRepository(private val jdbc: JdbcTemplate, private val media: MediaRepository) {
    fun checkCapacity(projectId: UUID) {
        media.checkProject(projectId)
        if (jdbc.queryForObject("SELECT count(*) FROM visual_references WHERE project_id = ?", Long::class.java, projectId)!! >= 100) {
            throw MediaFailure("REFERENCE_LIMIT", "This project has reached its lifetime limit of 100 references.", 409)
        }
        if (jdbc.queryForObject("SELECT count(*) FROM visual_references WHERE project_id = ? AND archived_at IS NULL", Long::class.java, projectId)!! >= 8) {
            throw MediaFailure("ACTIVE_REFERENCE_LIMIT", "Archive a reference before adding another. At most 8 references can be active.", 409)
        }
    }

    @Transactional(readOnly = true)
    fun list(projectId: UUID): List<ReferenceView> {
        media.checkProject(projectId)
        return jdbc.query("SELECT * FROM visual_references WHERE project_id = ? AND archived_at IS NULL ORDER BY created_at, id LIMIT 8", { row, _ -> view(row) }, projectId)
    }

    fun get(projectId: UUID, referenceId: UUID): ReferenceView = jdbc.query(
        "SELECT * FROM visual_references WHERE project_id = ? AND id = ?", { row, _ -> view(row) }, projectId, referenceId,
    ).firstOrNull() ?: throw MediaFailure("REFERENCE_NOT_FOUND", "This reference does not exist in this project.", 404)

    @Transactional
    fun create(projectId: UUID, id: UUID, title: String, guidance: String, image: PreparedFrame, hash: String): ReferenceView {
        jdbc.queryForObject("SELECT id FROM projects WHERE id = ? FOR UPDATE", UUID::class.java, projectId)
        checkCapacity(projectId)
        jdbc.update("INSERT INTO visual_references (id, project_id, title, guidance, width, height, sha256) VALUES (?, ?, ?, ?, ?, ?, ?)",
            id, projectId, title.trim(), guidance.trim(), image.width, image.height, hash)
        return get(projectId, id)
    }

    @Transactional
    fun update(projectId: UUID, id: UUID, request: ReferenceMetadataRequest): ReferenceView {
        get(projectId, id)
        if (jdbc.update("UPDATE visual_references SET title = ?, guidance = ? WHERE project_id = ? AND id = ? AND archived_at IS NULL", request.title.trim(), request.guidance.trim(), projectId, id) != 1) {
            throw MediaFailure("REFERENCE_ARCHIVED", "Archived reference metadata cannot be edited.", 409)
        }
        return get(projectId, id)
    }

    @Transactional
    fun archive(projectId: UUID, id: UUID): ReferenceView {
        get(projectId, id)
        jdbc.update("UPDATE visual_references SET archived_at = CURRENT_TIMESTAMP WHERE project_id = ? AND id = ? AND archived_at IS NULL", projectId, id)
        return get(projectId, id)
    }

    @Transactional(readOnly = true)
    fun findingReferences(projectId: UUID, findingId: UUID): List<ReferenceView> {
        if (jdbc.queryForObject("SELECT count(*) FROM findings WHERE project_id = ? AND id = ?", Long::class.java, projectId, findingId) != 1L) {
            throw MediaFailure("FINDING_NOT_FOUND", "This finding does not exist in this project.", 404)
        }
        return jdbc.query("""
            SELECT reference.id, snapshot.project_id, snapshot.title, snapshot.guidance, snapshot.width,
                snapshot.height, snapshot.sha256, reference.created_at, reference.archived_at
            FROM finding_references link
            JOIN analysis_references snapshot USING (analysis_run_id, reference_id, project_id)
            JOIN visual_references reference ON reference.id = snapshot.reference_id AND reference.project_id = snapshot.project_id
            WHERE link.project_id = ? AND link.finding_id = ? ORDER BY snapshot.position
        """.trimIndent(), { row, _ -> view(row) }, projectId, findingId)
    }

    private fun view(row: ResultSet): ReferenceView {
        val id = row.getObject("id", UUID::class.java)
        val projectId = row.getObject("project_id", UUID::class.java)
        return ReferenceView(id, projectId, row.getString("title"), row.getString("guidance"), row.getInt("width"), row.getInt("height"),
            row.getString("sha256"), row.getTimestamp("created_at").toInstant(), row.getTimestamp("archived_at")?.toInstant(),
            "/api/v1/projects/$projectId/references/$id/content")
    }
}
