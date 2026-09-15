package dev.sceneproof

import dev.sceneproof.analysis.*
import dev.sceneproof.film.*
import dev.sceneproof.media.MediaProcess
import dev.sceneproof.media.MediaStorage
import dev.sceneproof.project.CreateProjectRequest
import dev.sceneproof.project.ProjectService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "film-test")
class FilmUnderstandingApiTest @Autowired constructor(
    private val mvc: MockMvc, private val mapper: ObjectMapper, private val jdbc: JdbcTemplate,
    private val projects: ProjectService, private val repository: FilmRepository, private val sources: SourceFilmService,
    private val service: FilmUnderstandingService, private val decisions: FilmCandidateService,
    private val assembler: AnalysisContextAssembler, private val storage: MediaStorage, private val process: MediaProcess,
    private val analyses: AnalysisRepository, private val resultValidator: ContinuityResultValidator,
    private val references: dev.sceneproof.reference.ReferenceRepository,
    private val demos: dev.sceneproof.demo.DemoService,
) {
    @MockitoBean lateinit var filmPort: FilmUnderstandingPort
    @MockitoBean lateinit var audioPort: AudioTranscriptionPort
    @MockitoBean lateinit var continuityPort: ContinuityAnalysisPort
    @MockitoSpyBean lateinit var storageSpy: MediaStorage
    private lateinit var source: SourceFilm
    private val releases = mutableListOf<CountDownLatch>()

    companion object {
        @TempDir @JvmStatic lateinit var directory: Path
        @DynamicPropertySource @JvmStatic fun properties(registry: DynamicPropertyRegistry) { registry.add("sceneproof.media.root") { directory.resolve("media").toString() } }
    }

    @BeforeEach fun setup() {
        cleanup()
        doAnswer { FilmFixtures.completion(it.getArgument(0)) }.`when`(filmPort).understand(anyFilm())
        doReturn(TranscriptionCompletion(listOf(TranscribedSegment(100, 1200, "An original test narration")), "req_transcript_fixture")).`when`(audioPort).transcribe(anyAudio())
        val project = projects.create(CreateProjectRequest("Film test", "", ""))
        source = sources.upload(project.id, video())
    }

    @AfterEach fun cleanup() {
        releases.forEach { it.countDown() }
        check(jdbc.queryForObject("SELECT current_schema()", String::class.java) == "sceneproof_film_test")
        repeat(100) {
            if (jdbc.queryForObject("SELECT count(*) FROM film_understanding_runs WHERE completed_at IS NULL", Long::class.java) == 0L) return@repeat
            Thread.sleep(30)
        }
        jdbc.execute("TRUNCATE film_candidates, film_transcript_segments, film_segments, film_understanding_stages, film_understanding_runs, source_films, demo_replacements, finding_references, analysis_references, visual_references, targeted_results, finding_action_shots, finding_actions, finding_frames, finding_shots, findings, analysis_runs, frames, shots, projects")
    }

    private fun video(audio: Boolean = true): MockMultipartFile {
        val path = directory.resolve("${UUID.randomUUID()}.mp4")
        val arguments = mutableListOf("ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-f", "lavfi", "-i", "color=c=red:s=160x90:r=12:d=3")
        if (audio) arguments.addAll(listOf("-f", "lavfi", "-i", "sine=frequency=440:duration=3", "-c:a", "aac"))
        arguments.addAll(listOf("-c:v", "libx264", "-pix_fmt", "yuv420p", "-t", "3", "-threads", "2", path.toString()))
        process.run(arguments, 15)
        return MockMultipartFile("file", "original.mp4", "video/mp4", Files.readAllBytes(path))
    }

    private fun anyFilm(): FilmUnderstandingContext = ArgumentMatchers.any(FilmUnderstandingContext::class.java) ?: FilmUnderstandingContext(SourceFilm(UUID(0, 0), UUID(0, 0), "", "", 1, 1, java.time.Instant.EPOCH), emptyList(), emptyList(), emptyList(), "")
    private fun anyAudio(): AudioInput = ArgumentMatchers.any(AudioInput::class.java) ?: AudioInput(ByteArray(0), 0)
    private fun start(request: UUID = UUID.randomUUID()) = service.start(source.projectId, source.id, request)
    private fun await(run: FilmRun): FilmRun {
        repeat(200) {
            val current = repository.get(run.projectId, run.id)
            if (current.completedAt != null) return current
            Thread.sleep(50)
        }
        error("Film worker did not terminate")
    }

    @Test fun `source upload preserves identity and hash without any provider work`() {
        val bytes = Files.readAllBytes(storage.file(source.projectId, source.id, "original.bin"))
        val replay = sources.upload(source.projectId, MockMultipartFile("file", "different-name.mp4", "text/plain", bytes))
        assertThat(replay.id).isEqualTo(source.id)
        assertThat(replay.durationMs).isBetween(2900, 3100)
        verifyNoInteractions(filmPort, audioPort, continuityPort)
        mvc.multipart("/api/v1/projects/${source.projectId}/film/source") { file(MockMultipartFile("file", "fake.mp4", "video/mp4", byteArrayOf(1, 2))) }.andExpect { status { isUnsupportedMediaType() } }
        assertThat(repository.source(source.projectId)).isEqualTo(source)
    }

    @Test fun `project deletion removes discovery decisions references and owned media and supports replay`() {
        val run = await(start())
        val candidate = repository.candidates(source.projectId, run.id).first()
        decisions.decide(source.projectId, candidate.id, DecideCandidateRequest(CandidateStatus.ACCEPTED))
        val promoted = decisions.promote(source.projectId, candidate.id)
        val context = assembler.assemble(source.projectId)
        val finding = ContinuityFinding(FindingCategory.PROP, FindingSeverity.MEDIUM, 0.7, "Delete fixture", "Potential difference", "Red", "Changed", "Review context", context.shots.map { it.id }, context.shots.flatMap { it.frames.map { frame -> frame.id } }, listOf(promoted.referenceId!!), "Keep red", emptyList())
        val result = ContinuityAnalysisResult("Deletion fixture", listOf(finding), context.shots.map { InspectedShot(it.id, it.frames.map { frame -> frame.id }) }, emptyList(), AnalysisMetadata(source.projectId, "1", "SAMPLED_SEQUENCE"))
        val analysis = analyses.start(source.projectId, UUID.randomUUID()).first
        analyses.recordContext(analysis.id, context)
        analyses.succeed(analysis.id, context, AnalysisCompletion(result, ANALYSIS_MODEL, null, null, null))
        val savedFinding = analyses.findings(source.projectId, analysis.id, 0).single()
        val targeted = analyses.start(source.projectId, UUID.randomUUID()).first
        jdbc.update("UPDATE analysis_runs SET kind = 'TARGETED' WHERE id = ?", targeted.id)
        analyses.recordContext(targeted.id, context)
        analyses.succeed(targeted.id, context, AnalysisCompletion(result.copy(findings = emptyList()), ANALYSIS_MODEL, null, null, null))
        val action = UUID.randomUUID()
        jdbc.update("INSERT INTO finding_actions (id, project_id, finding_id, original_analysis_run_id, request_id, action_type, explanation, scope, reanalysis_run_id) VALUES (?, ?, ?, ?, ?, 'INTENTIONAL_CHANGE', 'Creator explanation', 'Original scope', ?)", action, source.projectId, savedFinding.id, analysis.id, UUID.randomUUID(), targeted.id)
        jdbc.update("INSERT INTO finding_action_shots (action_id, finding_id, project_id, shot_id) VALUES (?, ?, ?, ?)", action, savedFinding.id, source.projectId, context.shots.first().id)
        jdbc.update("INSERT INTO targeted_results (action_id, outcome, result) VALUES (?, 'INTENT_ACCEPTED', '{}'::jsonb)", action)
        val other = projects.create(CreateProjectRequest("Keep this project"))
        val otherFile = storage.file(other.id, UUID.randomUUID(), "original.bin")
        Files.write(otherFile, byteArrayOf(7))
        val projectDirectory = storage.directory(source.projectId, source.id).parent
        assertThatThrownBy { jdbc.update("DELETE FROM film_candidates WHERE id = ?", candidate.id) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
        repeat(2) {
            mvc.delete("/api/v1/projects/${source.projectId}") { contentType = MediaType.APPLICATION_JSON; content = """{"confirmationName":"Film test"}""" }.andExpect { status { isNoContent() } }
        }
        mvc.get("/api/v1/projects/${source.projectId}").andExpect { status { isNotFound() } }
        listOf("projects", "source_films", "film_understanding_runs", "film_understanding_stages", "film_transcript_segments", "film_candidates", "film_segments", "visual_references", "shots", "frames", "analysis_runs", "findings", "finding_shots", "finding_frames", "finding_actions", "finding_action_shots", "targeted_results", "analysis_references", "finding_references").forEach { table ->
            val expected = if (table == "projects") 1L else 0L
            assertThat(jdbc.queryForObject("SELECT count(*) FROM $table", Long::class.java)).describedAs(table).isEqualTo(expected)
        }
        assertThat(Files.exists(projectDirectory)).isFalse()
        assertThat(Files.readAllBytes(otherFile)).containsExactly(7)
        assertThatThrownBy { storage.directory(source.projectId, UUID.randomUUID()) }.isInstanceOf(IllegalArgumentException::class.java)
        verify(filmPort, times(1)).understand(anyFilm()); verify(audioPort, times(1)).transcribe(anyAudio())
    }

    @Test fun `failed media cleanup reports pending state and retries after project rows are gone`() {
        doThrow(IllegalStateException("test cleanup failure")).doCallRealMethod().`when`(storageSpy).deleteProject(source.projectId)
        mvc.delete("/api/v1/projects/${source.projectId}") { contentType = MediaType.APPLICATION_JSON; content = """{"confirmationName":"Film test"}""" }.andExpect { status { isServiceUnavailable() }; jsonPath("$.detail") { value("Project records were deleted, but media cleanup is pending. Retry deletion to finish cleanup safely.") } }
        mvc.get("/api/v1/projects/${source.projectId}").andExpect { status { isNotFound() } }
        assertThat(jdbc.queryForObject("SELECT media_cleaned_at IS NULL FROM project_deletions WHERE project_id = ?", Boolean::class.java, source.projectId)).isTrue()
        mvc.delete("/api/v1/projects/${source.projectId}") { contentType = MediaType.APPLICATION_JSON; content = """{"confirmationName":"Film test"}""" }.andExpect { status { isNoContent() } }
        assertThat(jdbc.queryForObject("SELECT media_cleaned_at IS NOT NULL FROM project_deletions WHERE project_id = ?", Boolean::class.java, source.projectId)).isTrue()
        verifyNoInteractions(filmPort, audioPort, continuityPort)
    }

    @Test fun `project deletion rejects missing confirmation demos and active runs without provider work`() {
        mvc.delete("/api/v1/projects/${source.projectId}") { contentType = MediaType.APPLICATION_JSON; content = """{"confirmationName":"wrong name"}""" }.andExpect { status { isBadRequest() } }
        mvc.delete("/api/v1/projects/${UUID.randomUUID()}") { contentType = MediaType.APPLICATION_JSON; content = """{"confirmationName":"Film test"}""" }.andExpect { status { isNotFound() } }
        val demo = projects.createDemo(CreateProjectRequest("Demo"), UUID.randomUUID(), "test")
        mvc.delete("/api/v1/projects/${demo.id}") { contentType = MediaType.APPLICATION_JSON; content = """{"confirmationName":"Demo"}""" }.andExpect { status { isConflict() } }
        val active = repository.start(source.projectId, source.id, UUID.randomUUID()).first
        mvc.delete("/api/v1/projects/${source.projectId}") { contentType = MediaType.APPLICATION_JSON; content = """{"confirmationName":"Film test"}""" }.andExpect { status { isConflict() } }
        repository.fail(active.id, AnalysisFailure("TEST_FINISHED", "Test finished"))
        assertThat(projects.get(source.projectId).name).isEqualTo("Film test")
        verifyNoInteractions(filmPort, audioPort, continuityPort)
    }

    @Test fun `consent is required and cross project source cannot start work`() {
        mvc.post("/api/v1/projects/${source.projectId}/film/runs") { contentType = MediaType.APPLICATION_JSON; content = """{"requestId":"${UUID.randomUUID()}","sourceFilmId":"${source.id}","paidConsent":false}""" }.andExpect { status { isBadRequest() } }
        val other = projects.create(CreateProjectRequest("Other film"))
        mvc.post("/api/v1/projects/${other.id}/film/runs") { contentType = MediaType.APPLICATION_JSON; content = """{"requestId":"${UUID.randomUUID()}","sourceFilmId":"${source.id}","paidConsent":true}""" }.andExpect { status { isNotFound() } }
        verifyNoInteractions(filmPort, audioPort)
    }

    @Test fun `real extraction persists stages transcript candidates and source timestamps`() {
        val run = await(start())
        assertThat(run.stage).isEqualTo(FilmStage.SUCCEEDED)
        assertThat(run.reasoning).isEqualTo("medium")
        assertThat(jdbc.queryForObject("SELECT count(*) FROM paid_run_reservations WHERE run_id = ?", Long::class.java, run.id)).isEqualTo(1)
        assertThat(run.transcriptionModel).isEqualTo("gpt-4o-transcribe-diarize")
        assertThat(run.stages.map { it.stage }).containsExactly(*FilmStage.entries.filter { it != FilmStage.FAILED }.toTypedArray())
        assertThat(run.stages.all { it.completedAt != null }).isTrue()
        assertThat(repository.transcript(source.projectId, run.id).single().startMs).isEqualTo(100)
        assertThat(repository.transcript(source.projectId, run.id).single().timestampOrigin).isEqualTo("OPENAI_DIARIZED_SEGMENT_ESTIMATE_SOURCE_START")
        assertThat(repository.segments(source.projectId)).hasSizeBetween(1, 8)
        assertThat(repository.segments(source.projectId).flatMap { it.shot.frames }.size).isLessThanOrEqualTo(24)
        assertThat(repository.candidates(source.projectId, run.id)).allMatch { it.status == CandidateStatus.PENDING }
        assertThat(repository.memory(source.projectId).anchors).isEmpty()
        verify(filmPort, times(1)).understand(anyFilm()); verify(audioPort, times(1)).transcribe(anyAudio())
        mvc.get("/api/v1/projects/${source.projectId}/film/runs/${run.id}/transcript").andExpect { status { isOk() }; header { string("Cache-Control", "no-store") } }
    }

    @Test fun `active run survives reads and same request replay without duplicate inference`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1).also { releases.add(it) }
        doAnswer { entered.countDown(); check(release.await(15, TimeUnit.SECONDS)); FilmFixtures.completion(it.getArgument(0)) }.`when`(filmPort).understand(anyFilm())
        val run = start()
        assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue()
        assertThat(repository.get(source.projectId, run.id).stage).isEqualTo(FilmStage.UNDERSTANDING_FILM)
        assertThat(start(run.requestId).id).isEqualTo(run.id)
        assertThatThrownBy { start() }.isInstanceOfSatisfying(AnalysisFailure::class.java) { assertThat(it.code).isEqualTo("FILM_BUSY") }
        release.countDown()
        assertThat(await(run).stage).isEqualTo(FilmStage.SUCCEEDED)
        assertThat(start(run.requestId).id).isEqualTo(run.id)
        verify(filmPort, times(1)).understand(anyFilm()); verify(audioPort, times(1)).transcribe(anyAudio())
    }

    @Test fun `transcription failure remains durable and prevents understanding call`() {
        val failure = OpenAiAudioTranscriptionAdapter("").incompleteResponse(null, cause = javax.net.ssl.SSLHandshakeException("Private transport information"))
        doThrow(failure).`when`(audioPort).transcribe(anyAudio())
        val run = await(start())
        assertThat(run.stage).isEqualTo(FilmStage.FAILED)
        assertThat(run.failureMessage).contains("TLS_FAILURE", "No upstream response headers").doesNotContain("Private transport information")
        assertThat(jdbc.queryForObject("SELECT count(*) FROM paid_run_reservations WHERE run_id = ?", Long::class.java, run.id)).isEqualTo(1)
        assertThat(start(run.requestId).id).isEqualTo(run.id)
        assertThat(repository.candidates(source.projectId, run.id)).isEmpty()
        verifyNoInteractions(filmPort)
        verify(audioPort, times(1)).transcribe(anyAudio())
    }

    @Test fun `sanitized provider rejection survives worker persistence and read replay`() {
        val failure = OpenAiAudioTranscriptionAdapter("").rejection(400, """{"error":{"type":"invalid_request_error","code":"invalid_value","message":"Invalid file format."}}""".toByteArray(), "req_saved_rejection")
        doThrow(failure).`when`(audioPort).transcribe(anyAudio())
        val run = await(start())
        assertThat(run.stage).isEqualTo(FilmStage.FAILED)
        assertThat(repository.get(source.projectId, run.id).failureMessage).isEqualTo(failure.detail)
        assertThat(start(run.requestId).providerRequestId).isEqualTo("req_saved_rejection")
        assertThat(run.failureMessage).contains("HTTP 400", "type=invalid_request_error", "code=invalid_value", "Invalid file format.")
        mvc.get("/api/v1/projects/${source.projectId}/film/runs/${run.id}").andExpect {
            status { isOk() }; jsonPath("$.failureMessage") { value(failure.detail) }; jsonPath("$.providerRequestId") { value("req_saved_rejection") }
        }
        verify(audioPort, times(1)).transcribe(anyAudio()); verifyNoInteractions(filmPort)
    }

    @Test fun `invalid provider identifiers fail atomically after transcription without candidates`() {
        doAnswer { FilmFixtures.completion(it.getArgument(0)).let { value -> value.copy(result = value.result.copy(sourceFilmId = UUID.randomUUID())) } }.`when`(filmPort).understand(anyFilm())
        val run = await(start())
        assertThat(run.failureCode).isEqualTo("INVALID_FILM_OUTPUT")
        assertThat(run.providerResponseId).isEqualTo("resp_film_fixture")
        assertThat(repository.candidates(source.projectId, run.id)).isEmpty()
        assertThat(repository.transcript(source.projectId, run.id)).hasSize(1)
    }

    @Test fun `creator decisions are separate replay safe and promotion uses normal references`() {
        val run = await(start())
        val candidates = repository.candidates(source.projectId, run.id)
        assertThatThrownBy { decisions.promote(source.projectId, candidates[0].id) }.isInstanceOf(AnalysisFailure::class.java)
        val accept = DecideCandidateRequest(CandidateStatus.ACCEPTED)
        decisions.decide(source.projectId, candidates[0].id, accept)
        assertThat(decisions.decide(source.projectId, candidates[0].id, accept).status).isEqualTo(CandidateStatus.ACCEPTED)
        decisions.decide(source.projectId, candidates[1].id, DecideCandidateRequest(CandidateStatus.EDITED, "Creator title", "Creator rule", "Creator scope"))
        decisions.decide(source.projectId, candidates[2].id, DecideCandidateRequest(CandidateStatus.REJECTED))
        val memory = assembler.assemble(source.projectId).filmMemory
        assertThat(memory.anchors).hasSize(2)
        assertThat(memory.anchors.map { it.rule }).contains("Creator rule").doesNotContain(candidates[2].proposal.rule)
        assertThat(memory.evidence.map { it.kind }).contains("TRANSCRIPT", "NARRATIVE_INTERPRETATION")
        val promoted = decisions.promote(source.projectId, candidates[0].id)
        assertThat(promoted.referenceId).isNotNull()
        assertThat(references.get(source.projectId, promoted.referenceId!!).filmProvenance?.candidateId).isEqualTo(candidates[0].id)
        java.util.concurrent.Executors.newFixedThreadPool(5).use { executor ->
            val ready = CountDownLatch(5)
            val reads = (1..5).map { executor.submit<UUID> { ready.countDown(); check(ready.await(5, TimeUnit.SECONDS)); references.get(source.projectId, promoted.referenceId).filmProvenance!!.candidateId } }
            assertThat(reads.map { it.get(5, TimeUnit.SECONDS) }).containsOnly(candidates[0].id)
        }
        assertThatThrownBy { jdbc.update("UPDATE film_candidates SET confirmed_rule = 'Rewritten' WHERE id = ?", candidates[0].id) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
        assertThatThrownBy { jdbc.update("UPDATE film_candidates SET proposal = '{}'::jsonb WHERE id = ?", candidates[0].id) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
        assertThatThrownBy { jdbc.update("UPDATE film_understanding_runs SET result = '{}'::jsonb WHERE id = ?", run.id) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
        assertThat(decisions.promote(source.projectId, candidates[0].id).referenceId).isEqualTo(promoted.referenceId)
        assertThat(jdbc.queryForObject("SELECT count(*) FROM visual_references WHERE project_id = ?", Long::class.java, source.projectId)).isEqualTo(1)
        verify(filmPort, times(1)).understand(anyFilm()); verify(audioPort, times(1)).transcribe(anyAudio()); verifyNoInteractions(continuityPort)
    }

    @Test fun `expired run remains failed and late stage updates cannot overwrite it`() {
        val run = repository.start(source.projectId, source.id, UUID.randomUUID()).first
        jdbc.update("UPDATE film_understanding_runs SET started_at = CURRENT_TIMESTAMP - INTERVAL '11 minutes' WHERE id = ?", run.id)
        assertThat(repository.get(source.projectId, run.id).failureCode).isEqualTo("FILM_INTERRUPTED")
        assertThatThrownBy { repository.advance(run.id, FilmStage.PREPARING_SOURCE, FilmStage.DETECTING_STRUCTURE) }.isInstanceOf(AnalysisFailure::class.java)
        assertThat(start(run.requestId).stage).isEqualTo(FilmStage.FAILED)
        verifyNoInteractions(filmPort, audioPort)
    }

    @Test fun `expired but live worker blocks another provider worker and destructive cleanup`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1).also { releases.add(it) }
        doAnswer {
            entered.countDown()
            check(release.await(15, TimeUnit.SECONDS))
            TranscriptionCompletion(emptyList(), "req_fixture")
        }.`when`(audioPort).transcribe(anyAudio())
        val original = start()
        assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue()
        try {
            jdbc.update("UPDATE film_understanding_runs SET started_at = CURRENT_TIMESTAMP - INTERVAL '11 minutes' WHERE id = ?", original.id)
            assertThat(repository.get(source.projectId, original.id).failureCode).isEqualTo("FILM_INTERRUPTED")
            val next = await(start())
            assertThat(next.failureCode).isEqualTo("ANALYSIS_BUSY")
            mvc.delete("/api/v1/projects/${source.projectId}") {
                contentType = MediaType.APPLICATION_JSON; content = """{"confirmationName":"Film test"}"""
            }.andExpect { status { isConflict() } }
            verify(audioPort, times(1)).transcribe(anyAudio())
            verifyNoInteractions(filmPort)
        } finally { release.countDown() }
    }

    @Test fun `failed film frame copy removes staging and unpublished segment directories`() {
        doAnswer { invocation ->
            val path = invocation.callRealMethod()
            if (invocation.getArgument<String>(2) == "00.png") throw IllegalStateException("Fixture copy failure")
            path
        }.`when`(storageSpy).file(ArgumentMatchers.eq(source.projectId) ?: source.projectId, ArgumentMatchers.any(UUID::class.java) ?: UUID(0, 0), ArgumentMatchers.anyString() ?: "")
        val run = await(start())
        assertThat(run.stage).isEqualTo(FilmStage.FAILED)
        val owned = directory.resolve("media").resolve(source.projectId.toString())
        assertThat(Files.list(owned).use { it.map { path -> path.fileName.toString() }.toList() }).containsExactly(source.id.toString())
        assertThat(repository.segments(source.projectId)).isEmpty()
        verifyNoInteractions(audioPort, filmPort)
    }

    @Test fun `fresh attempt reuses immutable structure without replaying old request or memory`() {
        val original = await(start())
        val segments = repository.segments(source.projectId)
        val candidate = repository.candidates(source.projectId, original.id).first()
        decisions.decide(source.projectId, candidate.id, DecideCandidateRequest(CandidateStatus.ACCEPTED))
        val next = await(start())
        assertThat(next.stage).isEqualTo(FilmStage.SUCCEEDED)
        assertThat(next.id).isNotEqualTo(original.id)
        assertThat(repository.segments(source.projectId)).isEqualTo(segments)
        assertThat(repository.memory(source.projectId).anchors.single().runId).isEqualTo(original.id)
        assertThat(repository.transcript(source.projectId, next.id).map { it.id }).doesNotContainAnyElementsOf(repository.transcript(source.projectId, original.id).map { it.id })
        assertThat(start(original.requestId).id).isEqualTo(original.id)
        verify(filmPort, times(2)).understand(anyFilm()); verify(audioPort, times(2)).transcribe(anyAudio())
    }

    @Test fun `cross modal findings and original memory retain immutable provenance after later discovery`() {
        val discovery = await(start())
        decisions.decide(source.projectId, repository.candidates(source.projectId, discovery.id).first().id, DecideCandidateRequest(CandidateStatus.ACCEPTED))
        val context = assembler.assemble(source.projectId)
        val segments = repository.segments(source.projectId).associateBy { it.id }
        context.shots.forEach { shot -> shot.frames.forEach { frame ->
            assertThat(frame.timestampMs).isEqualTo(segments.getValue(shot.id).startMs + segments.getValue(shot.id).shot.frames.single { it.id == frame.id }.timestampMs!!)
        } }
        val finding = ContinuityFinding(FindingCategory.PROP, FindingSeverity.MEDIUM, 0.7, "Test evidence", "Cross-modal test", "Red shape", "Changed shape", "Narration is context, not automatic truth", context.shots.map { it.id }, context.shots.flatMap { it.frames.map { frame -> frame.id } }, emptyList(), "Keep the shape red", context.filmMemory.evidence.map { it.id })
        val result = ContinuityAnalysisResult("Test-only continuity", listOf(finding), context.shots.map { InspectedShot(it.id, it.frames.map { frame -> frame.id }) }, emptyList(), AnalysisMetadata(source.projectId, "1", "SAMPLED_SEQUENCE"))
        resultValidator.validate(result, context)
        assertThatThrownBy { resultValidator.validate(result.copy(findings = listOf(finding.copy(relevantFilmEvidenceIds = listOf(UUID.randomUUID())))), context) }.isInstanceOf(AnalysisFailure::class.java)
        val run = analyses.start(source.projectId, UUID.randomUUID()).first
        analyses.recordContext(run.id, context)
        analyses.succeed(run.id, context, AnalysisCompletion(result, ANALYSIS_MODEL, null, null, null))
        val saved = analyses.findings(source.projectId, run.id, 0).single()
        assertThat(saved.filmEvidence).isEqualTo(context.filmMemory.evidence)
        assertThat(saved.filmEvidence.map { it.kind }).containsExactly("TRANSCRIPT", "NARRATIVE_INTERPRETATION")
        assertThat(await(start()).stage).isEqualTo(FilmStage.SUCCEEDED)
        assertThat(assembler.assemble(source.projectId).filmMemory.evidence).isNotEqualTo(saved.filmEvidence)
        assertThat(analyses.originalFilmMemory(source.projectId, run.id)).isEqualTo(context.filmMemory)
        assertThat(analyses.finding(source.projectId, saved.id).filmEvidence).isEqualTo(saved.filmEvidence)
        verifyNoInteractions(continuityPort)
    }

    @Test fun `changed source bytes fail closed before either provider`() {
        Files.write(storage.file(source.projectId, source.id, "original.bin"), byteArrayOf(1, 2, 3))
        assertThat(await(start()).failureCode).isEqualTo("SOURCE_UNAVAILABLE")
        verifyNoInteractions(filmPort, audioPort)
    }

    @Test fun `silent video honestly skips transcription and still supports visual discovery`() {
        val other = projects.create(CreateProjectRequest("No audio"))
        source = sources.upload(other.id, video(false))
        val run = await(start())
        assertThat(run.stage).isEqualTo(FilmStage.SUCCEEDED)
        assertThat(run.audioStatus).isEqualTo("NO_AUDIO_STREAM")
        assertThat(repository.transcript(source.projectId, run.id)).isEmpty()
        verifyNoInteractions(audioPort)
        verify(filmPort, times(1)).understand(anyFilm())
    }

    @Test fun `run transcript candidate and source ownership are enforced`() {
        val run = await(start())
        val other = projects.create(CreateProjectRequest("Other owner"))
        val candidate = repository.candidates(source.projectId, run.id).first()
        listOf("runs/${run.id}", "runs/${run.id}/transcript", "runs/${run.id}/candidates").forEach { path -> mvc.get("/api/v1/projects/${other.id}/film/$path").andExpect { status { isNotFound() } } }
        assertThatThrownBy { decisions.decide(other.id, candidate.id, DecideCandidateRequest(CandidateStatus.ACCEPTED)) }.isInstanceOf(AnalysisFailure::class.java)
        assertThatThrownBy { repository.start(source.projectId, UUID.randomUUID(), run.requestId) }.isInstanceOfSatisfying(AnalysisFailure::class.java) { assertThat(it.code).isEqualTo("REQUEST_CONFLICT") }
    }

    @Test fun `derived demo source and reset preserve master history and deterministic bounded media`() {
        val packaged = dev.sceneproof.demo.PackagedDemoTemplate(mapper)
        val template = packaged.template()
        val provenance = requireNotNull(template.sourceProvenance)
        val masterProject = projects.create(CreateProjectRequest("Historical master fixture"))
        val master = sources.upload(masterProject.id, packaged.upload(dev.sceneproof.demo.DemoAsset(provenance.masterFile, provenance.masterSha256, "Master")))
        assertThat(master.durationMs).isEqualTo(92459)
        val original = demos.open(UUID.randomUUID())
        val derived = requireNotNull(repository.source(original.id))
        assertThat(derived.sha256).isEqualTo(template.sourceFilm!!.sha256).isNotEqualTo(master.sha256)
        assertThat(derived.durationMs).isBetween(36291, 36293)

        fun preflight(film: SourceFilm): Pair<List<Pair<Long?, String>>, AudioInput> {
            val run = repository.start(film.projectId, film.id, UUID.randomUUID()).first
            val segments = sources.structure(film, run.id)
            val frames = sources.frames(film, segments)
            assertThat(segments).hasSizeBetween(1, 8)
            assertThat(segments).allMatch { it.startMs >= 0 && it.endMs <= film.durationMs }
            assertThat(frames).hasSizeBetween(1, 24).allMatch { it.timestampMs!! in 0 until film.durationMs }
            val audio = requireNotNull(sources.audio(film, run.id))
            assertThat(audio.durationMs).isBetween(36290, 36293)
            assertThat(audio.wav.size).isLessThan(4_000_000)
            assertThat(Files.exists(storage.directory(film.projectId, run.id).resolve("audio.wav"))).isFalse()
            repository.fail(run.id, AnalysisFailure("PREFLIGHT_ONLY", "Deterministic test; no provider calls."))
            return frames.map { it.timestampMs to dev.sceneproof.media.ImageContent.sha256(it.png) } to audio
        }

        val first = preflight(derived)
        val replacement = demos.reset(original.id)
        val replacementSource = requireNotNull(repository.source(replacement.id))
        val repeated = preflight(replacementSource)
        assertThat(repeated.first).isEqualTo(first.first)
        assertThat(repeated.second.wav).isEqualTo(first.second.wav)
        assertThat(repository.source(original.id)).isEqualTo(derived)
        assertThat(repository.source(masterProject.id)).isEqualTo(master)
        assertThat(provenance.masterPath).isEqualTo("demo/between the lines - demo.mp4")
        assertThat(provenance.sourceStartUs).isZero()
        assertThat(provenance.sourceEndUs).isEqualTo(36291667)
        verifyNoInteractions(filmPort, audioPort, continuityPort)
    }
}
