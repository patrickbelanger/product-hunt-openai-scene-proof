package dev.sceneproof.analysis

import dev.sceneproof.media.MediaRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class AnalysisRunView(
    val id: UUID, val projectId: UUID, val requestId: UUID, val status: String, val provider: String, val model: String,
    val startedAt: Instant, val completedAt: Instant?, val failureCode: String?, val failureMessage: String?,
    val shotCount: Int, val frameCount: Int, val projectSummary: String?, val warnings: List<String>,
    val usage: AnalysisUsage?, val providerResponseId: String?, val providerRequestId: String?,
)

data class FindingView(
    val id: UUID, val analysisRunId: UUID, val category: FindingCategory, val severity: FindingSeverity,
    val confidence: Double, val title: String, val summary: String, val expectedState: String, val observedState: String,
    val explanation: String, val affectedShotIds: List<UUID>, val relevantFrameIds: List<UUID>,
    val relevantReferenceIds: List<UUID>, val suggestedCorrectionPrompt: String, val status: String,
)

@Repository
class AnalysisRepository(private val jdbc: JdbcTemplate, private val mapper: ObjectMapper, private val media: MediaRepository) {
    @Transactional
    fun start(projectId: UUID, requestId: UUID): Pair<AnalysisRunView, Boolean> {
        media.checkProject(projectId)
        jdbc.execute("SELECT pg_advisory_xact_lock(731204)")
        expireInterrupted()
        findRequest(projectId, requestId)?.let { return it to false }
        if (jdbc.queryForObject("SELECT count(*) FROM analysis_runs WHERE status = 'RUNNING'", Long::class.java) != 0L) {
            throw AnalysisFailure("ANALYSIS_BUSY", "Another analysis is running. Retry this request later with the same requestId.", 429)
        }
        if (jdbc.queryForObject("SELECT count(*) FROM analysis_runs WHERE project_id = ?", Long::class.java, projectId)!! >= 100) {
            throw AnalysisFailure("ANALYSIS_RUN_LIMIT", "This project has reached the local limit of 100 analysis attempts.", 409)
        }
        val id = UUID.randomUUID()
        jdbc.update("INSERT INTO analysis_runs (id, project_id, request_id, status, model, started_at) VALUES (?, ?, ?, 'RUNNING', ?, CURRENT_TIMESTAMP)", id, projectId, requestId, ANALYSIS_MODEL)
        return get(projectId, id) to true
    }

    fun findRequest(projectId: UUID, requestId: UUID): AnalysisRunView? = jdbc.query(
        "SELECT * FROM analysis_runs WHERE project_id = ? AND request_id = ?", { row, _ -> runView(row) }, projectId, requestId,
    ).firstOrNull()

    @Transactional
    fun get(projectId: UUID, id: UUID): AnalysisRunView {
        media.checkProject(projectId)
        expireInterrupted()
        return jdbc.query("SELECT * FROM analysis_runs WHERE project_id = ? AND id = ?", { row, _ -> runView(row) }, projectId, id)
            .firstOrNull() ?: throw AnalysisFailure("ANALYSIS_NOT_FOUND", "This analysis does not exist in this project.", 404)
    }

    @Transactional
    fun recordContext(id: UUID, context: AnalysisContext) {
        val snapshot = mapOf(
            "schemaVersion" to "1", "strategy" to "first-middle-last-v1", "projectId" to context.projectId,
            "name" to context.name, "description" to context.description, "rules" to context.rules,
            "shots" to context.shots.map { shot -> mapOf(
                "id" to shot.id, "position" to shot.position, "name" to shot.name, "kind" to shot.kind, "durationMs" to shot.durationMs,
                "availableFrameCount" to shot.availableFrameCount, "frames" to shot.frames.map { frame -> mapOf(
                    "id" to frame.id, "position" to frame.position, "timestampMs" to frame.timestampMs, "width" to frame.width, "height" to frame.height,
                    "sha256" to java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(frame.png)),
                ) },
            ) },
        )
        requireRunning(jdbc.update("UPDATE analysis_runs SET context = ?::jsonb, shot_count = ?, frame_count = ?, warnings = ?::jsonb WHERE id = ? AND status = 'RUNNING'",
            mapper.writeValueAsString(snapshot), context.shots.size, context.shots.sumOf { it.frames.size }, mapper.writeValueAsString(context.warnings), id))
    }

    @Transactional
    fun succeed(id: UUID, context: AnalysisContext, completion: AnalysisCompletion) {
        requireRunning(jdbc.update("UPDATE analysis_runs SET status = 'SUCCEEDED', completed_at = CURRENT_TIMESTAMP, project_summary = ?, warnings = ?::jsonb, usage = ?::jsonb, provider_response_id = ?, provider_request_id = ? WHERE id = ? AND status = 'RUNNING'",
            completion.result.projectSummary, mapper.writeValueAsString((context.warnings + completion.result.warnings).distinct()), completion.usage?.let(mapper::writeValueAsString), completion.providerResponseId, completion.providerRequestId, id))
        val frameOwners = context.shots.flatMap { shot -> shot.frames.map { it.id to shot.id } }.toMap()
        completion.result.findings.forEachIndexed { position, finding ->
            val findingId = UUID.randomUUID()
            jdbc.update("INSERT INTO findings (id, analysis_run_id, project_id, category, severity, confidence, title, summary, expected_state, observed_state, explanation, suggested_correction_prompt, position) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                findingId, id, context.projectId, finding.category.name, finding.severity.name, finding.confidence, finding.title, finding.summary, finding.expectedState, finding.observedState, finding.explanation, finding.suggestedCorrectionPrompt, position)
            finding.affectedShotIds.forEach { jdbc.update("INSERT INTO finding_shots (finding_id, shot_id, project_id) VALUES (?, ?, ?)", findingId, it, context.projectId) }
            finding.relevantFrameIds.forEach { jdbc.update("INSERT INTO finding_frames (finding_id, frame_id, shot_id) VALUES (?, ?, ?)", findingId, it, frameOwners.getValue(it)) }
        }
    }

    @Transactional
    fun fail(id: UUID, failure: AnalysisFailure) {
        jdbc.update("UPDATE analysis_runs SET status = 'FAILED', completed_at = CURRENT_TIMESTAMP, failure_code = ?, failure_message = ?, usage = ?::jsonb, provider_response_id = ?, provider_request_id = ? WHERE id = ? AND status = 'RUNNING'",
            failure.code, failure.detail, failure.usage?.let(mapper::writeValueAsString), failure.providerResponseId, failure.providerRequestId, id)
    }

    @Transactional(readOnly = true)
    fun findings(projectId: UUID, analysisId: UUID?, page: Int): List<FindingView> {
        media.checkProject(projectId)
        if (analysisId != null && jdbc.queryForObject("SELECT count(*) FROM analysis_runs WHERE project_id = ? AND id = ?", Long::class.java, projectId, analysisId) != 1L) throw AnalysisFailure("ANALYSIS_NOT_FOUND", "This analysis does not exist in this project.", 404)
        return jdbc.query("SELECT findings.* FROM findings JOIN analysis_runs ON analysis_runs.id = findings.analysis_run_id WHERE findings.project_id = ? AND analysis_runs.status = 'SUCCEEDED' AND (?::uuid IS NULL OR analysis_run_id = ?) ORDER BY analysis_runs.started_at DESC, analysis_runs.id, findings.position LIMIT 20 OFFSET ?", { row, _ ->
            val id = row.getObject("id", UUID::class.java)
            FindingView(id, row.getObject("analysis_run_id", UUID::class.java), FindingCategory.valueOf(row.getString("category")), FindingSeverity.valueOf(row.getString("severity")), row.getDouble("confidence"), row.getString("title"), row.getString("summary"), row.getString("expected_state"), row.getString("observed_state"), row.getString("explanation"),
                jdbc.query("SELECT shot_id FROM finding_shots JOIN shots ON shots.id = finding_shots.shot_id WHERE finding_id = ? ORDER BY shots.position", { shot, _ -> shot.getObject("shot_id", UUID::class.java) }, id),
                jdbc.query("SELECT frame_id FROM finding_frames JOIN frames ON frames.id = finding_frames.frame_id JOIN shots ON shots.id = frames.shot_id WHERE finding_id = ? ORDER BY shots.position, frames.position", { frame, _ -> frame.getObject("frame_id", UUID::class.java) }, id),
                emptyList(), row.getString("suggested_correction_prompt"), row.getString("status"))
        }, projectId, analysisId, analysisId, page * 20)
    }

    private fun expireInterrupted() {
        jdbc.update("UPDATE analysis_runs SET status = 'FAILED', completed_at = CURRENT_TIMESTAMP, failure_code = 'ANALYSIS_INTERRUPTED', failure_message = 'The analysis did not finish before its recovery deadline. No automatic retry was made.' WHERE status = 'RUNNING' AND started_at < CURRENT_TIMESTAMP - INTERVAL '5 minutes'")
    }

    private fun requireRunning(updated: Int) {
        if (updated != 1) throw AnalysisFailure("ANALYSIS_INTERRUPTED", "This analysis is no longer running. No findings were saved.", 409)
    }

    private fun runView(row: ResultSet) = AnalysisRunView(
        row.getObject("id", UUID::class.java), row.getObject("project_id", UUID::class.java), row.getObject("request_id", UUID::class.java), row.getString("status"), row.getString("provider"), row.getString("model"), row.getTimestamp("started_at").toInstant(), row.getTimestamp("completed_at")?.toInstant(), row.getString("failure_code"), row.getString("failure_message"), row.getInt("shot_count"), row.getInt("frame_count"), row.getString("project_summary"), mapper.readTree(row.getString("warnings")).asSequence().map { it.asString() }.toList(), row.getString("usage")?.let { mapper.readValue(it, AnalysisUsage::class.java) }, row.getString("provider_response_id"), row.getString("provider_request_id"),
    )
}
