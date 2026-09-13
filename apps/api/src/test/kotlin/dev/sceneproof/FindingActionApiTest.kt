package dev.sceneproof

import dev.sceneproof.analysis.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
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
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import org.springframework.transaction.support.TransactionSynchronizationManager
import tools.jackson.databind.ObjectMapper
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.util.UUID
import javax.imageio.ImageIO

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "steering-test")
class FindingActionApiTest @Autowired constructor(private val mvc: MockMvc, private val mapper: ObjectMapper, private val jdbc: JdbcTemplate, private val analyses: AnalysisRepository, private val assembler: AnalysisContextAssembler) {
    @MockitoBean lateinit var port: ContinuityAnalysisPort
    @MockitoSpyBean lateinit var actions: FindingActionRepository
    private val projects = mutableListOf<UUID>()
    private lateinit var projectId: UUID
    private lateinit var finding: FindingView
    private lateinit var captured: TargetedContext
    private var calls = 0
    private var outcome = IntentOutcome.INTENT_ACCEPTED
    private var failure: AnalysisFailure? = null
    private var transform: (TargetedResult) -> TargetedResult = { it }

    companion object {
        @TempDir @JvmStatic lateinit var directory: Path
        @DynamicPropertySource @JvmStatic fun properties(registry: DynamicPropertyRegistry) {
            registry.add("sceneproof.media.root") { directory.resolve("media").toString() }
        }
    }

    private fun anyContext(): TargetedContext = org.mockito.ArgumentMatchers.any(TargetedContext::class.java)
        ?: TargetedContext(AnalysisContext(UUID(0, 0), "", "", "", emptyList(), emptyList()), finding, "", "", emptyList(), null)

    @BeforeEach
    fun setup() {
        projectId = project()
        repeat(3) { image(projectId) }
        val context = assembler.assemble(projectId)
        val run = analyses.start(projectId, UUID.randomUUID()).first
        val selected = context.shots.take(2)
        analyses.recordContext(run.id, context)
        analyses.succeed(run.id, context, AnalysisCompletion(ContinuityAnalysisResult("Original fixture finding", listOf(ContinuityFinding(FindingCategory.PROP, FindingSeverity.HIGH, 0.9, "Color drift", "Red becomes blue", "Red", "Blue", "The square should stay red", selected.map { it.id }, selected.flatMap { shot -> shot.frames.map { it.id } }, emptyList(), "Keep the square red")), context.shots.map { InspectedShot(it.id, it.frames.map { frame -> frame.id }) }, emptyList(), AnalysisMetadata(projectId, "1", "SAMPLED_SEQUENCE")), ANALYSIS_MODEL, null, null, null))
        finding = analyses.findings(projectId, run.id, 0).single()
        `when`(port.reanalyze(anyContext())).thenAnswer { invocation ->
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()
            calls++
            captured = invocation.getArgument(0)
            failure?.let { throw it }
            completion(captured).let { it.copy(result = transform(it.result)) }
        }
    }

    @AfterEach
    fun finishInterruptedFixtures() {
        check(jdbc.queryForObject("SELECT current_schema()", String::class.java) == "sceneproof_steering_test")
        jdbc.execute("TRUNCATE targeted_results, finding_action_shots, finding_actions, finding_frames, finding_shots, findings, analysis_runs, frames, shots, projects")
    }

    private fun project(): UUID = mvc.post("/api/v1/projects") {
        contentType = MediaType.APPLICATION_JSON; content = """{"name":"PR4 deterministic verification","rules":"The square stays red unless a narrative transition explains it."}"""
    }.andExpect { status { isCreated() } }.andReturn().response.let { UUID.fromString(mapper.readTree(it.contentAsString)["id"].asString()) }.also { projects.add(it) }

    private fun image(project: UUID) = mvc.multipart("/api/v1/projects/$project/shots") {
        val bytes = ByteArrayOutputStream().use { ImageIO.write(BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB), "png", it); it.toByteArray() }
        file(MockMultipartFile("file", "original.png", "image/png", bytes))
    }.andExpect { status { isCreated() } }

    private fun request(type: FindingActionType = FindingActionType.INTENTIONAL_CHANGE) = CreateFindingActionRequest(UUID.randomUUID(), type, "The prop is repainted after arriving home.", "Between the two affected shots after a time jump.", finding.affectedShotIds)
    private fun post(body: CreateFindingActionRequest, project: UUID = projectId, id: UUID = finding.id) = mvc.post("/api/v1/projects/$project/findings/$id/actions") { contentType = MediaType.APPLICATION_JSON; content = mapper.writeValueAsString(body) }
    private fun completion(context: TargetedContext) = TargetedCompletion(TargetedResult(projectId, finding.id, "1", outcome, "Independent judgement", "The evidence and declared narrative boundary were evaluated.", context.scope, context.affectedShotIds, context.originalFinding.relevantFrameIds, context.sequence.shots.map { InspectedShot(it.id, it.frames.map { frame -> frame.id }) }, if (outcome == IntentOutcome.ISSUE_REMAINS) "The rule is still contradicted." else "", if (outcome == IntentOutcome.ISSUE_REMAINS) "Keep the square red." else ""), ANALYSIS_MODEL, "resp_test_targeted", "req_test_targeted", AnalysisUsage(100, 50, 150, 0, 20))

    @Test
    fun `accepted intent is durable immutable and replay safe with original evidence preserved`() {
        val body = request()
        val saved = post(body).andExpect { status { isOk() }; jsonPath("$.result.outcome") { value("INTENT_ACCEPTED") }; jsonPath("$.reanalysis.status") { value("SUCCEEDED") } }.andReturn().response
        post(body).andExpect { content { json(saved.contentAsString) } }
        mvc.get(saved.getHeader("Location")!!).andExpect { content { json(saved.contentAsString) } }
        assertThat(calls).isEqualTo(1)
        assertThat(analyses.finding(projectId, finding.id)).isEqualTo(finding.copy(status = "INTENTIONAL"))
        assertThat(jdbc.queryForObject("SELECT status FROM findings WHERE id = ?", String::class.java, finding.id)).isEqualTo("OPEN")
        val action = actions.history(projectId, finding.id).single()
        assertThat(action.originalAnalysisRunId).isEqualTo(finding.analysisRunId)
        assertThat(action.explanation).isEqualTo(body.explanation)
        assertThat(action.affectedShotIds).containsExactlyElementsOf(finding.affectedShotIds)
        assertThat(action.reanalysis!!.usage!!.totalTokens).isEqualTo(150)
        assertThatThrownBy { jdbc.update("UPDATE finding_actions SET explanation = 'rewritten' WHERE id = ?", action.id) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
        assertThatThrownBy { jdbc.update("DELETE FROM targeted_results WHERE action_id = ?", action.id) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
    }

    @Test
    fun `issue remains and insufficient evidence keep open and subsequent judgement explicitly supersedes`() {
        outcome = IntentOutcome.ISSUE_REMAINS
        post(request()).andExpect { status { isOk() }; jsonPath("$.result.remainingIssue") { isNotEmpty() } }
        val first = actions.history(projectId, finding.id).single()
        assertThat(analyses.finding(projectId, finding.id).status).isEqualTo("OPEN")
        outcome = IntentOutcome.INSUFFICIENT_EVIDENCE
        post(request()).andExpect { status { isOk() }; jsonPath("$.result.outcome") { value("INSUFFICIENT_EVIDENCE") }; jsonPath("$.supersedesActionId") { value(first.id.toString()) } }
        assertThat(captured.previousJudgement).isEqualTo(first.result)
        assertThat(analyses.finding(projectId, finding.id)).isEqualTo(finding)
        assertThat(actions.history(projectId, finding.id)).hasSize(2)
    }

    @Test
    fun `targeted assembly uses original evidence affected shots and immediate neighbor despite larger project`() {
        repeat(7) { image(projectId) }
        post(request()).andExpect { status { isOk() } }
        assertThat(captured.sequence.shots).hasSize(3)
        assertThat(captured.originalFinding).isEqualTo(finding)
        assertThat(captured.sequence.rules).contains("narrative transition")
        assertThat(captured.sequence.shots.flatMap { it.frames.map { frame -> frame.id } }).containsAll(finding.relevantFrameIds)
        val run = actions.history(projectId, finding.id).single().reanalysis!!
        val snapshot = jdbc.queryForObject("SELECT context::text FROM analysis_runs WHERE id = ?", String::class.java, run.id)!!
        assertThat(snapshot).contains("sha256").doesNotContain("base64", "data:image", "storage_key")
    }

    @Test
    fun `blank oversized duplicate and foreign scope are rejected without provider or action`() {
        val valid = request()
        listOf(valid.copy(explanation = " "), valid.copy(explanation = "x".repeat(2001)), valid.copy(scope = ""), valid.copy(scope = "x".repeat(1001)), valid.copy(affectedShotIds = emptyList())).forEach { post(it).andExpect { status { isBadRequest() } } }
        listOf(valid.copy(affectedShotIds = listOf(UUID.randomUUID())), valid.copy(affectedShotIds = finding.affectedShotIds + finding.affectedShotIds.first())).forEach { post(it).andExpect { status { isUnprocessableEntity() } } }
        assertThat(actions.history(projectId, finding.id)).isEmpty()
        assertThat(calls).isZero()
    }

    @Test
    fun `request conflict and cross project isolation never admit second inference`() {
        val body = request()
        post(body).andExpect { status { isOk() } }
        post(body.copy(explanation = "Different explanation")).andExpect { status { isConflict() } }
        post(body, project()).andExpect { status { isNotFound() } }
        mvc.get("/api/v1/projects/${project()}/findings/${finding.id}/actions").andExpect { status { isNotFound() } }
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `provider refusal failure timeout and invalid output persist failure without optimistic acceptance`() {
        listOf("PROVIDER_REFUSAL", "PROVIDER_UNAVAILABLE", "PROVIDER_TIMEOUT", "PROVIDER_INCOMPLETE", "INVALID_MODEL_OUTPUT").forEach { code ->
            failure = AnalysisFailure(code, "Safe provider failure", 502, AnalysisUsage(5, 2, 7, null, null))
            val body = request()
            post(body).andExpect { status { isBadGateway() } }
            val beforeReplay = calls
            post(body).andExpect { status { isOk() }; jsonPath("$.reanalysis.status") { value("FAILED") }; jsonPath("$.result") { doesNotExist() } }
            assertThat(calls).isEqualTo(beforeReplay)
            assertThat(analyses.finding(projectId, finding.id)).isEqualTo(finding)
        }
        assertThat(actions.history(projectId, finding.id)).hasSize(5)
    }

    @Test
    fun `foreign project finding shot frame and incomplete coverage are durable invalid output`() {
        listOf<(TargetedResult) -> TargetedResult>(
            { it.copy(projectId = UUID.randomUUID()) }, { it.copy(originalFindingId = UUID.randomUUID()) },
            { it.copy(affectedShotIds = listOf(UUID.randomUUID())) }, { it.copy(evidenceFrameIds = it.evidenceFrameIds + UUID.randomUUID()) },
            { it.copy(inspectedShots = it.inspectedShots.dropLast(1)) }, { it.copy(evidenceFrameIds = it.evidenceFrameIds.dropLast(1)) },
        ).forEach { invalid -> transform = invalid; post(request()).andExpect { status { isBadGateway() } }; assertThat(analyses.finding(projectId, finding.id)).isEqualTo(finding) }
    }

    @Test
    fun `resolve is durable creator correction without model approval or provider call`() {
        val body = request(FindingActionType.RESOLVE)
        post(body).andExpect { status { isOk() }; jsonPath("$.reanalysis") { doesNotExist() } }
        post(body).andExpect { status { isOk() } }
        assertThat(analyses.finding(projectId, finding.id)).isEqualTo(finding.copy(status = "RESOLVED"))
        post(request()).andExpect { status { isConflict() } }
        post(request(FindingActionType.DISMISS)).andExpect { status { isConflict() } }
        assertThat(calls).isZero()
    }

    @Test
    fun `dismiss preserves history and differs from intentional or resolved`() {
        post(request(FindingActionType.DISMISS)).andExpect { status { isOk() }; jsonPath("$.result") { doesNotExist() } }
        assertThat(analyses.finding(projectId, finding.id)).isEqualTo(finding.copy(status = "DISMISSED"))
        post(request(FindingActionType.RESOLVE)).andExpect { status { isConflict() } }
        assertThat(calls).isZero()
    }

    @Test
    fun `pending intent blocks other actions shares global admission and replays without work`() {
        val body = request()
        val (pending) = actions.start(projectId, finding.id, body)
        post(body).andExpect { status { isOk() }; jsonPath("$.reanalysis.status") { value("RUNNING") } }
        post(request(FindingActionType.RESOLVE)).andExpect { status { isConflict() } }
        assertThatThrownBy { analyses.start(project(), UUID.randomUUID()) }.isInstanceOfSatisfying(AnalysisFailure::class.java) { assertThat(it.code).isEqualTo("ANALYSIS_BUSY") }
        jdbc.update("UPDATE analysis_runs SET started_at = CURRENT_TIMESTAMP - INTERVAL '6 minutes' WHERE id = ?", pending.reanalysis!!.id)
        mvc.get("/api/v1/projects/$projectId/findings/${finding.id}/actions").andExpect { jsonPath("$[0].reanalysis.status") { value("FAILED") } }
        post(body).andExpect { status { isOk() }; jsonPath("$.reanalysis.failureCode") { value("ANALYSIS_INTERRUPTED") } }
        assertThat(analyses.finding(projectId, finding.id)).isEqualTo(finding)
        assertThat(calls).isZero()
    }

    @Test
    fun `simultaneous same request admits one provider call and exposes durable running action`() {
        val entered = java.util.concurrent.CountDownLatch(1)
        val finish = java.util.concurrent.CountDownLatch(1)
        doAnswer { invocation ->
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()
            calls++
            entered.countDown()
            check(finish.await(10, java.util.concurrent.TimeUnit.SECONDS))
            completion(invocation.getArgument(0))
        }.`when`(port).reanalyze(anyContext())
        val body = request()
        java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val first = executor.submit { post(body).andExpect { status { isOk() } } }
            try {
                check(entered.await(10, java.util.concurrent.TimeUnit.SECONDS))
                post(body).andExpect { status { isOk() }; jsonPath("$.reanalysis.status") { value("RUNNING") } }
                post(request()).andExpect { status { isConflict() } }
            } finally { finish.countDown() }
            first.get(10, java.util.concurrent.TimeUnit.SECONDS)
        }
        assertThat(calls).isEqualTo(1)
        assertThat(actions.history(projectId, finding.id)).hasSize(1)
    }

    @Test
    fun `final persistence failure rolls back run success and leaves prior judgement effective`() {
        val (action) = actions.start(projectId, finding.id, request())
        val sequence = assembler.assemble(projectId, finding)
        val context = TargetedContext(sequence, finding, action.explanation, action.scope, action.affectedShotIds, null)
        assertThatThrownBy { actions.succeed(action.copy(id = UUID.randomUUID()), sequence, completion(context)) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
        assertThat(analyses.get(projectId, action.reanalysis!!.id).status).isEqualTo("RUNNING")
        assertThat(actions.get(projectId, finding.id, action.id).result).isNull()
        assertThat(analyses.finding(projectId, finding.id)).isEqualTo(finding)
        analyses.fail(action.reanalysis.id, AnalysisFailure("ANALYSIS_PERSISTENCE_FAILURE", "Could not persist"))
    }

    @Test
    fun `service finalization error records durable failure and usage with replay safe`() {
        doThrow(org.springframework.dao.DataIntegrityViolationException("test-only")).`when`(actions).succeed(
            org.mockito.ArgumentMatchers.any(FindingActionView::class.java) ?: FindingActionView(UUID.randomUUID(), projectId, finding.id, finding.analysisRunId, UUID.randomUUID(), FindingActionType.INTENTIONAL_CHANGE, "Fixture", "Fixture", finding.affectedShotIds, java.time.Instant.now(), null, null, null),
            org.mockito.ArgumentMatchers.any(AnalysisContext::class.java) ?: AnalysisContext(projectId, "", "", "", emptyList(), emptyList()),
            org.mockito.ArgumentMatchers.any(TargetedCompletion::class.java) ?: completion(TargetedContext(AnalysisContext(projectId, "", "", "", emptyList(), emptyList()), finding, "", "", finding.affectedShotIds, null)),
        )
        val body = request()
        post(body).andExpect { status { isServiceUnavailable() } }
        post(body).andExpect { status { isOk() }; jsonPath("$.reanalysis.failureCode") { value("ANALYSIS_PERSISTENCE_FAILURE") }; jsonPath("$.reanalysis.usage.totalTokens") { value(150) } }
        assertThat(calls).isEqualTo(1)
        assertThat(analyses.finding(projectId, finding.id)).isEqualTo(finding)
    }
}
