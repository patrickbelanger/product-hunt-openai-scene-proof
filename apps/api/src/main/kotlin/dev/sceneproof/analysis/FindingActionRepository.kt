package dev.sceneproof.analysis

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.util.UUID

@Repository
class FindingActionRepository(private val jdbc: JdbcTemplate, private val mapper: ObjectMapper, private val analyses: AnalysisRepository) {
    @Transactional
    fun start(projectId: UUID, findingId: UUID, request: CreateFindingActionRequest): Pair<FindingActionView, Boolean> {
        jdbc.execute("SELECT pg_advisory_xact_lock(731204)")
        val original = analyses.finding(projectId, findingId)
        jdbc.queryForObject("SELECT id FROM findings WHERE id = ? FOR UPDATE", UUID::class.java, findingId)
        val existing = jdbc.query("SELECT * FROM finding_actions WHERE project_id = ? AND request_id = ?", { row, _ -> view(row) }, projectId, request.requestId).firstOrNull()
        if (existing != null) {
            if (existing.findingId != findingId || existing.type != request.type || existing.explanation != request.explanation.trim() || existing.scope != request.scope.trim() || existing.affectedShotIds.toSet() != request.affectedShotIds.toSet()) {
                throw AnalysisFailure("REQUEST_CONFLICT", "This requestId already belongs to a different action. Replay the original request unchanged.", 409)
            }
            return existing to false
        }
        if (request.affectedShotIds.size != original.affectedShotIds.size || request.affectedShotIds.toSet() != original.affectedShotIds.toSet()) {
            throw AnalysisFailure("INVALID_ACTION_SCOPE", "The declared scope must include exactly this finding's affected shots.", 422)
        }
        val history = history(projectId, findingId)
        if (history.any { it.reanalysis?.status == "RUNNING" }) throw AnalysisFailure("FINDING_BUSY", "This finding is being re-evaluated. Refresh its history before another action.", 409)
        if (analyses.finding(projectId, findingId).status != "OPEN") throw AnalysisFailure("FINDING_NOT_OPEN", "Only an open finding can receive a new creator action.", 409)
        if (history.size >= 100) throw AnalysisFailure("FINDING_ACTION_LIMIT", "This finding has reached the local limit of 100 creator actions.", 409)
        if (analyses.findRequest(projectId, request.requestId) != null) throw AnalysisFailure("REQUEST_CONFLICT", "This requestId already belongs to an analysis.", 409)
        val run = if (request.type == FindingActionType.INTENTIONAL_CHANGE) analyses.start(projectId, request.requestId).first else null
        if (run != null) jdbc.update("UPDATE analysis_runs SET kind = 'TARGETED' WHERE id = ?", run.id)
        val previous = history.lastOrNull { it.reanalysis == null || it.reanalysis.status == "SUCCEEDED" }
        val id = UUID.randomUUID()
        jdbc.update("INSERT INTO finding_actions (id, project_id, finding_id, original_analysis_run_id, request_id, action_type, explanation, scope, reanalysis_run_id, supersedes_action_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, projectId, findingId, original.analysisRunId, request.requestId, request.type.name, request.explanation.trim(), request.scope.trim(), run?.id, previous?.id)
        request.affectedShotIds.forEach { shotId -> jdbc.update("INSERT INTO finding_action_shots (action_id, finding_id, project_id, shot_id) VALUES (?, ?, ?, ?)", id, findingId, projectId, shotId) }
        return get(projectId, findingId, id) to true
    }

    @Transactional
    fun history(projectId: UUID, findingId: UUID): List<FindingActionView> {
        analyses.finding(projectId, findingId)
        return jdbc.query("SELECT * FROM finding_actions WHERE project_id = ? AND finding_id = ? ORDER BY created_at, id", { row, _ -> view(row) }, projectId, findingId)
    }

    @Transactional
    fun get(projectId: UUID, findingId: UUID, actionId: UUID): FindingActionView {
        analyses.finding(projectId, findingId)
        return jdbc.query("SELECT * FROM finding_actions WHERE project_id = ? AND finding_id = ? AND id = ?", { row, _ -> view(row) }, projectId, findingId, actionId)
            .firstOrNull() ?: throw AnalysisFailure("ACTION_NOT_FOUND", "This action does not exist for this finding.", 404)
    }

    @Transactional
    fun succeed(action: FindingActionView, context: AnalysisContext, completion: TargetedCompletion) {
        val runId = action.reanalysis!!.id
        analyses.succeedTargeted(runId, context, completion)
        jdbc.update("INSERT INTO targeted_results (action_id, outcome, result) VALUES (?, ?, ?::jsonb)", action.id, completion.result.outcome.name, mapper.writeValueAsString(completion.result))
    }

    private fun view(row: ResultSet): FindingActionView {
        val id = row.getObject("id", UUID::class.java)
        val projectId = row.getObject("project_id", UUID::class.java)
        val result = jdbc.query("SELECT result::text FROM targeted_results WHERE action_id = ?", { resultRow, _ -> mapper.readValue(resultRow.getString(1), TargetedResult::class.java) }, id).firstOrNull()
        return FindingActionView(id, projectId, row.getObject("finding_id", UUID::class.java), row.getObject("original_analysis_run_id", UUID::class.java), row.getObject("request_id", UUID::class.java), FindingActionType.valueOf(row.getString("action_type")), row.getString("explanation"), row.getString("scope"),
            jdbc.query("SELECT shot_id FROM finding_action_shots JOIN shots ON shots.id = shot_id WHERE action_id = ? ORDER BY shots.position", { shot, _ -> shot.getObject("shot_id", UUID::class.java) }, id),
            row.getTimestamp("created_at").toInstant(), row.getObject("supersedes_action_id", UUID::class.java), row.getObject("reanalysis_run_id", UUID::class.java)?.let { analyses.get(projectId, it) }, result)
    }
}
