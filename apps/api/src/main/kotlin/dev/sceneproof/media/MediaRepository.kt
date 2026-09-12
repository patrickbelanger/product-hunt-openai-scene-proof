package dev.sceneproof.media

import dev.sceneproof.project.ProjectNotFound
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

data class FrameView(val id: UUID, val position: Int, val timestampMs: Long?, val width: Int, val height: Int, val url: String)
data class ShotView(val id: UUID, val position: Int, val name: String, val kind: String, val status: String, val failureCode: String?, val durationMs: Long?, val frames: List<FrameView>)

@Repository
class MediaRepository(private val jdbc: JdbcTemplate) {
    fun checkProject(projectId: UUID) {
        if (jdbc.queryForObject("SELECT count(*) FROM projects WHERE id = ?", Long::class.java, projectId) != 1L) throw ProjectNotFound()
    }

    fun checkCapacity(projectId: UUID) {
        checkProject(projectId)
        if (jdbc.queryForObject("SELECT count(*) FROM shots WHERE project_id = ?", Long::class.java, projectId)!! >= 100) {
            throw MediaFailure("SHOT_LIMIT", "This project has reached its limit of 100 import attempts.", 409)
        }
    }

    @Transactional(readOnly = true)
    fun list(projectId: UUID): List<ShotView> {
        checkProject(projectId)
        return jdbc.query("SELECT * FROM shots WHERE project_id = ? ORDER BY position", { row, _ ->
            val shotId = row.getObject("id", UUID::class.java)
            ShotView(shotId, row.getInt("position"), row.getString("name"), row.getString("kind"), row.getString("status"), row.getString("failure_code"), row.getObject("duration_ms", java.lang.Long::class.java)?.toLong(),
                jdbc.query("SELECT * FROM frames WHERE shot_id = ? ORDER BY position", { frame, _ ->
                    val frameId = frame.getObject("id", UUID::class.java)
                    FrameView(frameId, frame.getInt("position"), frame.getObject("timestamp_ms", java.lang.Long::class.java)?.toLong(), frame.getInt("width"), frame.getInt("height"), "/api/v1/projects/$projectId/frames/$frameId/content")
                }, shotId))
        }, projectId)
    }

    @Transactional
    fun save(projectId: UUID, shotId: UUID, name: String, media: PreparedMedia?, failure: String?): ShotView {
        jdbc.queryForObject("SELECT id FROM projects WHERE id = ? FOR UPDATE", UUID::class.java, projectId) ?: throw ProjectNotFound()
        checkCapacity(projectId)
        val position = jdbc.queryForObject("SELECT COALESCE(MAX(position), -1) + 1 FROM shots WHERE project_id = ?", Int::class.java, projectId)!!
        jdbc.update("INSERT INTO shots (id, project_id, position, name, kind, status, failure_code, duration_ms, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            shotId, projectId, position, name, media?.kind ?: "UNKNOWN", if (failure == null) "READY" else "FAILED", failure, media?.durationMs, java.sql.Timestamp.from(Instant.now()))
        val frames = media?.frames.orEmpty().mapIndexed { index, frame ->
            val frameId = UUID.randomUUID()
            jdbc.update("INSERT INTO frames (id, shot_id, position, timestamp_ms, width, height, storage_key) VALUES (?, ?, ?, ?, ?, ?, ?)", frameId, shotId, index, frame.timestampMs, frame.width, frame.height, frame.key)
            FrameView(frameId, index, frame.timestampMs, frame.width, frame.height, "/api/v1/projects/$projectId/frames/$frameId/content")
        }
        return ShotView(shotId, position, name, media?.kind ?: "UNKNOWN", if (failure == null) "READY" else "FAILED", failure, media?.durationMs, frames)
    }

    fun content(projectId: UUID, frameId: UUID): Pair<UUID, String> = jdbc.query(
        "SELECT shot_id, storage_key FROM frames JOIN shots ON shots.id = frames.shot_id WHERE shots.project_id = ? AND frames.id = ? AND shots.status = 'READY'",
        { row, _ -> row.getObject("shot_id", UUID::class.java) to row.getString("storage_key") }, projectId, frameId,
    ).firstOrNull() ?: throw MediaFailure("FRAME_NOT_FOUND", "This frame does not exist in this project.", 404)
}
