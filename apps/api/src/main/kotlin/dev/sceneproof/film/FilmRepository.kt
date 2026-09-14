package dev.sceneproof.film

import dev.sceneproof.analysis.AnalysisFailure
import dev.sceneproof.analysis.AnalysisUsage
import dev.sceneproof.media.MediaRepository
import dev.sceneproof.media.PreparedMedia
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.sql.ResultSet
import java.util.UUID

@Repository
class FilmRepository(private val jdbc: JdbcTemplate, private val mapper: ObjectMapper, private val media: MediaRepository) {
    fun source(projectId: UUID): SourceFilm? {
        media.checkProject(projectId)
        return jdbc.query("SELECT * FROM source_films WHERE project_id = ?", { row, _ -> SourceFilm(row.uuid("id"), projectId, row.getString("name"), row.getString("sha256"), row.getLong("byte_size"), row.getLong("duration_ms"), row.getTimestamp("created_at").toInstant()) }, projectId).firstOrNull()
    }

    @Transactional
    fun saveSource(projectId: UUID, id: UUID, name: String, hash: String, size: Long, durationMs: Long): SourceFilm {
        jdbc.queryForObject("SELECT id FROM projects WHERE id = ? FOR UPDATE", UUID::class.java, projectId)
        source(projectId)?.let {
            if (it.sha256 != hash) throw AnalysisFailure("SOURCE_ALREADY_SET", "This project already has a primary source film. Create another project for a different film.", 409)
            return it
        }
        jdbc.update("INSERT INTO source_films (id, project_id, name, sha256, byte_size, duration_ms) VALUES (?, ?, ?, ?, ?, ?)", id, projectId, name, hash, size, durationMs)
        return requireNotNull(source(projectId))
    }

    @Transactional
    fun start(projectId: UUID, sourceId: UUID, requestId: UUID): Pair<FilmRun, Boolean> {
        jdbc.execute("SELECT pg_advisory_xact_lock(731204)")
        expire()
        val existing = jdbc.query("SELECT * FROM film_understanding_runs WHERE project_id = ? AND request_id = ?", { row, _ -> view(row) }, projectId, requestId).firstOrNull()
        if (existing != null) {
            if (existing.sourceFilmId != sourceId) throw AnalysisFailure("REQUEST_CONFLICT", "Replay the original source film and requestId.", 409)
            return existing to false
        }
        if (source(projectId)?.id != sourceId) throw AnalysisFailure("SOURCE_NOT_FOUND", "Select this project's primary source film.", 404)
        if (jdbc.queryForObject("SELECT count(*) FROM film_understanding_runs WHERE completed_at IS NULL", Long::class.java) != 0L ||
            jdbc.queryForObject("SELECT count(*) FROM analysis_runs WHERE status = 'RUNNING' AND started_at > CURRENT_TIMESTAMP - INTERVAL '5 minutes'", Long::class.java) != 0L) {
            throw AnalysisFailure("FILM_BUSY", "Another analysis is running. Recover the same request or wait for it to finish.", 429)
        }
        if (jdbc.queryForObject("SELECT count(*) FROM film_understanding_runs WHERE project_id = ?", Long::class.java, projectId)!! >= 10) throw AnalysisFailure("FILM_RUN_LIMIT", "This project has reached its limit of ten Film Understanding attempts.", 409)
        val id = UUID.randomUUID()
        jdbc.update("INSERT INTO film_understanding_runs (id, project_id, source_film_id, request_id, stage) VALUES (?, ?, ?, ?, 'PREPARING_SOURCE')", id, projectId, sourceId, requestId)
        jdbc.update("INSERT INTO film_understanding_stages (run_id, position, stage) VALUES (?, 0, 'PREPARING_SOURCE')", id)
        return get(projectId, id) to true
    }

    @Transactional
    fun list(projectId: UUID): List<FilmRun> {
        media.checkProject(projectId)
        expire()
        return jdbc.query("SELECT * FROM film_understanding_runs WHERE project_id = ? ORDER BY started_at DESC, id DESC LIMIT 10", { row, _ -> view(row) }, projectId)
    }

    @Transactional
    fun get(projectId: UUID, id: UUID): FilmRun {
        media.checkProject(projectId)
        expire()
        return jdbc.query("SELECT * FROM film_understanding_runs WHERE project_id = ? AND id = ?", { row, _ -> view(row) }, projectId, id).firstOrNull()
            ?: throw AnalysisFailure("FILM_RUN_NOT_FOUND", "This Film Understanding run does not exist in this project.", 404)
    }

    @Transactional
    fun advance(id: UUID, from: FilmStage, to: FilmStage) {
        require(to.ordinal == from.ordinal + 1 && to != FilmStage.SUCCEEDED)
        requireActive(jdbc.update("UPDATE film_understanding_runs SET stage = ?, updated_at = clock_timestamp() WHERE id = ? AND stage = ? AND completed_at IS NULL", to.name, id, from.name))
        jdbc.update("UPDATE film_understanding_stages SET completed_at = clock_timestamp() WHERE run_id = ? AND completed_at IS NULL", id)
        jdbc.update("INSERT INTO film_understanding_stages (run_id, position, stage) VALUES (?, ?, ?)", id, to.ordinal, to.name)
    }

    @Transactional
    fun structure(source: SourceFilm, prepared: List<Triple<UUID, Pair<Long, Long>, PreparedMedia>>): List<FilmSegment> {
        jdbc.queryForObject("SELECT id FROM source_films WHERE id = ? FOR UPDATE", UUID::class.java, source.id)
        if (segments(source.projectId).isNotEmpty()) return segments(source.projectId)
        require(prepared.size in 1..8)
        prepared.forEachIndexed { position, (shotId, bounds, mediaInput) ->
            media.save(source.projectId, shotId, "Film segment ${position + 1}", mediaInput, null)
            jdbc.update("INSERT INTO film_segments (source_film_id, project_id, shot_id, position, start_ms, end_ms) VALUES (?, ?, ?, ?, ?, ?)", source.id, source.projectId, shotId, position, bounds.first, bounds.second)
        }
        return segments(source.projectId)
    }

    fun segments(projectId: UUID): List<FilmSegment> {
        val shots = media.list(projectId).associateBy { it.id }
        return jdbc.query("SELECT * FROM film_segments WHERE project_id = ? ORDER BY position", { row, _ ->
            val id = row.uuid("shot_id")
            FilmSegment(id, row.getInt("position"), row.getLong("start_ms"), row.getLong("end_ms"), shots.getValue(id))
        }, projectId)
    }

    @Transactional
    fun audio(run: FilmRun, status: String, hash: String?, durationMs: Long?, completion: TranscriptionCompletion?) {
        requireActive(jdbc.update("UPDATE film_understanding_runs SET audio_status = ?, audio_sha256 = ?, audio_duration_ms = ?, transcription_request_id = ? WHERE id = ? AND stage = 'TRANSCRIBING_AUDIO'", status, hash, durationMs, completion?.providerRequestId, run.id))
        completion?.segments?.forEachIndexed { position, segment ->
            jdbc.update("INSERT INTO film_transcript_segments (id, run_id, project_id, position, start_ms, end_ms, text) VALUES (?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), run.id, run.projectId, position, segment.startMs, segment.endMs, segment.text)
        }
    }

    @Transactional
    fun transcript(projectId: UUID, runId: UUID): List<TranscriptSegment> {
        get(projectId, runId)
        return jdbc.query("SELECT * FROM film_transcript_segments WHERE project_id = ? AND run_id = ? ORDER BY position", { row, _ -> TranscriptSegment(row.uuid("id"), row.getLong("start_ms"), row.getLong("end_ms"), row.getString("text"), row.getString("timestamp_origin")) }, projectId, runId)
    }

    @Transactional
    fun succeed(run: FilmRun, completion: FilmUnderstandingCompletion) {
        requireActive(jdbc.update("UPDATE film_understanding_runs SET stage = 'SUCCEEDED', completed_at = clock_timestamp(), updated_at = clock_timestamp(), result = ?::jsonb, usage = ?::jsonb, provider_response_id = ?, provider_request_id = ? WHERE id = ? AND stage = 'BUILDING_CANDIDATES'", mapper.writeValueAsString(completion.result), completion.usage?.let(mapper::writeValueAsString), completion.providerResponseId, completion.providerRequestId, run.id))
        completion.result.candidates.forEachIndexed { position, candidate ->
            jdbc.update("INSERT INTO film_candidates (id, run_id, project_id, position, proposal) VALUES (?, ?, ?, ?, ?::jsonb)", UUID.randomUUID(), run.id, run.projectId, position, mapper.writeValueAsString(candidate))
        }
        finishStages(run.id, FilmStage.SUCCEEDED)
    }

    @Transactional
    fun fail(id: UUID, failure: AnalysisFailure) {
        if (jdbc.update("UPDATE film_understanding_runs SET stage = 'FAILED', completed_at = clock_timestamp(), updated_at = clock_timestamp(), failure_code = ?, failure_message = ?, usage = ?::jsonb, provider_response_id = ?, provider_request_id = ? WHERE id = ? AND completed_at IS NULL", failure.code, failure.detail.take(500), failure.usage?.let(mapper::writeValueAsString), failure.providerResponseId, failure.providerRequestId, id) == 1) finishStages(id, FilmStage.FAILED)
    }

    @Transactional
    fun candidates(projectId: UUID, runId: UUID): List<FilmCandidate> {
        get(projectId, runId)
        return jdbc.query("SELECT * FROM film_candidates WHERE project_id = ? AND run_id = ? ORDER BY position", { row, _ -> candidateView(row) }, projectId, runId)
    }

    fun candidate(projectId: UUID, id: UUID, lock: Boolean = false): FilmCandidate = jdbc.query("SELECT * FROM film_candidates WHERE project_id = ? AND id = ?${if (lock) " FOR UPDATE" else ""}", { row, _ -> candidateView(row) }, projectId, id).firstOrNull()
        ?: throw AnalysisFailure("CANDIDATE_NOT_FOUND", "This candidate does not exist in this project.", 404)

    fun decide(id: UUID, status: CandidateStatus, title: String?, rule: String?, scope: String?) {
        if (status in setOf(CandidateStatus.ACCEPTED, CandidateStatus.EDITED)) {
            val projectId = jdbc.queryForObject("SELECT project_id FROM film_candidates WHERE id = ?", UUID::class.java, id)!!
            jdbc.queryForObject("SELECT id FROM projects WHERE id = ? FOR UPDATE", UUID::class.java, projectId)
            if (jdbc.queryForObject("SELECT count(*) FROM film_candidates WHERE project_id = ? AND status IN ('ACCEPTED', 'EDITED')", Long::class.java, projectId)!! >= 24) throw AnalysisFailure("ANCHOR_LIMIT", "This project has reached its limit of 24 confirmed anchors.", 409)
        }
        requireActive(jdbc.update("UPDATE film_candidates SET status = ?, confirmed_title = ?, confirmed_rule = ?, confirmed_scope = ?, decided_at = clock_timestamp() WHERE id = ? AND status = 'PENDING'", status.name, title, rule, scope, id))
    }

    fun linkReference(id: UUID, referenceId: UUID) { jdbc.update("UPDATE film_candidates SET reference_id = ? WHERE id = ? AND reference_id IS NULL", referenceId, id) }

    @Transactional
    fun memory(projectId: UUID): ContinuityFilmMemory {
        val source = source(projectId) ?: return ContinuityFilmMemory()
        val run = list(projectId).firstOrNull { it.stage == FilmStage.SUCCEEDED } ?: return ContinuityFilmMemory()
        val anchors = jdbc.query("SELECT * FROM film_candidates WHERE project_id = ? AND status IN ('ACCEPTED', 'EDITED') ORDER BY decided_at, id LIMIT 24", { row, _ ->
            ConfirmedFilmAnchor(row.uuid("id"), source.id, row.uuid("run_id"), row.getString("confirmed_title"), row.getString("confirmed_rule"), row.getString("confirmed_scope"))
        }, projectId)
        val evidence = transcript(projectId, run.id).map { FilmContextEvidence(it.id, source.id, run.id, "TRANSCRIPT", it.text, it.startMs, it.endMs, it.timestampOrigin) } +
            run.result!!.narrativeCues.mapIndexed { index, cue -> FilmContextEvidence(UUID.nameUUIDFromBytes("${run.id}:narrative:$index".toByteArray()), source.id, run.id, "NARRATIVE_INTERPRETATION", "${cue.title}: ${cue.interpretation} Uncertainty: ${cue.uncertainty}", null, null, "MODEL_INTERPRETATION_NOT_A_TIMESTAMP", cue.evidence.frameIds, cue.evidence.transcriptSegmentIds) }
        return ContinuityFilmMemory(anchors, evidence)
    }

    private fun expire() {
        val ids = jdbc.query("UPDATE film_understanding_runs SET stage = 'FAILED', completed_at = clock_timestamp(), updated_at = clock_timestamp(), failure_code = 'FILM_INTERRUPTED', failure_message = 'This run exceeded its recovery deadline. No paid request was replayed. A new attempt requires confirmation.' WHERE completed_at IS NULL AND started_at < CURRENT_TIMESTAMP - INTERVAL '10 minutes' RETURNING id", { row, _ -> row.uuid("id") })
        ids.forEach { finishStages(it, FilmStage.FAILED) }
    }

    private fun finishStages(id: UUID, stage: FilmStage) {
        jdbc.update("UPDATE film_understanding_stages SET completed_at = clock_timestamp() WHERE run_id = ? AND completed_at IS NULL", id)
        jdbc.update("INSERT INTO film_understanding_stages (run_id, position, stage, completed_at) VALUES (?, ?, ?, clock_timestamp())", id, stage.ordinal, stage.name)
    }

    private fun requireActive(count: Int) { if (count != 1) throw AnalysisFailure("FILM_INTERRUPTED", "The run or candidate has already reached a terminal state.", 409) }
    private fun ResultSet.uuid(column: String): UUID = getObject(column, UUID::class.java)
    private fun candidateView(row: ResultSet) = FilmCandidate(row.uuid("id"), row.uuid("run_id"), mapper.readValue(row.getString("proposal"), CandidateAnchor::class.java), CandidateStatus.valueOf(row.getString("status")), row.getString("confirmed_title"), row.getString("confirmed_rule"), row.getString("confirmed_scope"), row.getTimestamp("decided_at")?.toInstant(), row.getObject("reference_id", UUID::class.java))
    private fun view(row: ResultSet): FilmRun {
        val id = row.uuid("id")
        val stages = jdbc.query("SELECT * FROM film_understanding_stages WHERE run_id = ? ORDER BY position", { stage, _ -> FilmStageView(FilmStage.valueOf(stage.getString("stage")), stage.getTimestamp("started_at").toInstant(), stage.getTimestamp("completed_at")?.toInstant()) }, id)
        return FilmRun(id, row.uuid("project_id"), row.uuid("source_film_id"), row.uuid("request_id"), FilmStage.valueOf(row.getString("stage")), row.getTimestamp("started_at").toInstant(), row.getTimestamp("completed_at")?.toInstant(), row.getString("failure_code"), row.getString("failure_message"), stages, row.getString("model"), row.getString("reasoning"), row.getString("transcription_model"), row.getString("audio_status"), row.getObject("audio_duration_ms", java.lang.Long::class.java)?.toLong(), row.getString("transcription_request_id"), row.getString("provider_response_id"), row.getString("provider_request_id"), row.getString("usage")?.let { mapper.readValue(it, AnalysisUsage::class.java) }, row.getString("result")?.let { mapper.readValue(it, FilmUnderstandingResult::class.java) })
    }
}
