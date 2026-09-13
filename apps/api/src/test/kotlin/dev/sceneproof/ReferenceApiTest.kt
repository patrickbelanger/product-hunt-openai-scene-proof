package dev.sceneproof

import dev.sceneproof.analysis.*
import dev.sceneproof.media.ImageContent
import dev.sceneproof.media.MediaStorage
import dev.sceneproof.reference.ReferenceMetadataRequest
import dev.sceneproof.reference.ReferenceRepository
import dev.sceneproof.reference.ReferenceView
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.transaction.support.TransactionSynchronizationManager
import tools.jackson.databind.ObjectMapper
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.imageio.ImageIO

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "reference-test")
class ReferenceApiTest @Autowired constructor(private val mvc: MockMvc, private val mapper: ObjectMapper, private val jdbc: JdbcTemplate, private val references: ReferenceRepository, private val assembler: AnalysisContextAssembler, private val analyses: AnalysisRepository, private val storage: MediaStorage) {
    @MockitoBean lateinit var port: ContinuityAnalysisPort
    private lateinit var projectId: UUID
    private lateinit var captured: AnalysisContext
    private lateinit var targeted: TargetedContext
    private var citations: List<UUID>? = null
    private var calls = 0

    companion object {
        @TempDir @JvmStatic lateinit var directory: Path
        @DynamicPropertySource @JvmStatic fun properties(registry: DynamicPropertyRegistry) {
            registry.add("sceneproof.media.root") { directory.resolve("media").toString() }
        }
    }

    private fun anyContext(): AnalysisContext = org.mockito.ArgumentMatchers.any(AnalysisContext::class.java) ?: AnalysisContext(UUID(0, 0), "", "", "", emptyList(), emptyList())
    private fun anyTarget(): TargetedContext = org.mockito.ArgumentMatchers.any(TargetedContext::class.java) ?: TargetedContext(AnalysisContext(UUID(0, 0), "", "", "", emptyList(), emptyList()), FindingView(UUID(0, 0), UUID(0, 0), FindingCategory.PROP, FindingSeverity.HIGH, 1.0, "", "", "", "", "", emptyList(), emptyList(), emptyList(), "", "OPEN"), "", "", emptyList(), null)

    @BeforeEach
    fun setup() {
        check(jdbc.queryForObject("SELECT current_schema()", String::class.java) == "sceneproof_reference_test")
        projectId = project()
        `when`(port.analyze(anyContext())).thenAnswer { invocation ->
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()
            captured = invocation.getArgument(0)
            calls++
            completion(captured)
        }
        `when`(port.reanalyze(anyTarget())).thenAnswer { invocation ->
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse()
            targeted = invocation.getArgument(0)
            calls++
            TargetedCompletion(TargetedResult(projectId, targeted.originalFinding.id, "1", IntentOutcome.ISSUE_REMAINS, "Original reference still applies", "The explanation does not settle this visual concern.", targeted.scope, targeted.affectedShotIds, targeted.originalFinding.relevantFrameIds, targeted.sequence.shots.map { InspectedShot(it.id, it.frames.map { frame -> frame.id }) }, "The original declared appearance differs.", "Match the original declared appearance."), ANALYSIS_MODEL, null, null, null)
        }
    }

    @AfterEach
    fun cleanup() {
        check(jdbc.queryForObject("SELECT current_schema()", String::class.java) == "sceneproof_reference_test")
        jdbc.execute("TRUNCATE finding_references, analysis_references, visual_references, targeted_results, finding_action_shots, finding_actions, finding_frames, finding_shots, findings, analysis_runs, frames, shots, projects")
    }

    private fun project(): UUID = UUID.fromString(mapper.readTree(mvc.post("/api/v1/projects") {
        contentType = MediaType.APPLICATION_JSON; content = """{"name":"PR5 deterministic verification","rules":"Original red rule"}"""
    }.andExpect { status { isCreated() } }.andReturn().response.contentAsString)["id"].asString())

    private fun image(width: Int = 128, height: Int = 128, format: String = "png"): ByteArray = ByteArrayOutputStream().use { output ->
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, output); output.toByteArray()
    }
    private fun upload(bytes: ByteArray = image(), project: UUID = projectId, title: String = "Original red prop", guidance: String = "Keep its red appearance.") = mvc.multipart("/api/v1/projects/$project/references") {
        file(MockMultipartFile("file", "../../private.png", "text/html", bytes)); param("title", title); param("guidance", guidance)
    }
    private fun reference(): ReferenceView = mapper.readValue(upload().andExpect { status { isCreated() } }.andReturn().response.contentAsString, ReferenceView::class.java)
    private fun shot() { mvc.multipart("/api/v1/projects/$projectId/shots") { file(MockMultipartFile("file", "observed.png", "image/png", image())) }.andExpect { status { isCreated() } } }
    private fun analyze(requestId: UUID = UUID.randomUUID()) = mvc.post("/api/v1/projects/$projectId/analyses") { contentType = MediaType.APPLICATION_JSON; content = """{"requestId":"$requestId"}""" }
    private fun completion(context: AnalysisContext) = AnalysisCompletion(ContinuityAnalysisResult("Reference fixture", listOf(ContinuityFinding(FindingCategory.PROP, FindingSeverity.HIGH, 0.9, "Declared appearance differs", "A contextual concern", "Red", "Blue", "The relevant declared appearance is red.", context.shots.map { it.id }, context.shots.flatMap { shot -> shot.frames.map { it.id } }, citations ?: context.references.map { it.id }, "Keep the declared appearance.")), context.shots.map { InspectedShot(it.id, it.frames.map { frame -> frame.id }) }, emptyList(), AnalysisMetadata(projectId, "1", "SAMPLED_SEQUENCE")), ANALYSIS_MODEL, "resp_reference_test", null, AnalysisUsage(100, 20, 120, 0, 0))

    @Test
    fun `PNG creates project scoped durable normalized reference without trusting mime or filenames`() {
        val saved = reference()
        assertThat(saved.title).isEqualTo("Original red prop")
        assertThat(saved.url).doesNotContain("private", "..")
        assertThat(references.list(projectId)).containsExactly(saved)
        mvc.get("/api/v1/projects/$projectId/references/${saved.id}").andExpect { status { isOk() }; jsonPath("$.id") { value(saved.id.toString()) } }
        val bytes = mvc.get(saved.url).andExpect { status { isOk() }; content { contentType(MediaType.IMAGE_PNG) }; header { string("X-Content-Type-Options", "nosniff") } }.andReturn().response.contentAsByteArray
        assertThat(ImageContent.sha256(bytes)).isEqualTo(saved.sha256)
        assertThat(ImageIO.read(bytes.inputStream()).width).isEqualTo(128)
        assertThat(Files.exists(directory.resolve("private.png"))).isFalse()
        assertThat(calls).isZero()
    }

    @Test
    fun `JPEG normalizes to bounded PNG`() {
        val response = upload(image(2000, 1000, "jpeg")).andExpect { status { isCreated() }; jsonPath("$.width") { value(1600) }; jsonPath("$.height") { value(800) } }.andReturn().response
        val saved = mapper.readValue(response.contentAsString, ReferenceView::class.java)
        val bytes = mvc.get(saved.url).andReturn().response.contentAsByteArray
        assertThat(bytes.take(8)).containsExactly(137.toByte(), 80, 78, 71, 13, 10, 26, 10)
    }

    @Test
    fun `corrupt and spoofed images fail without references or leftover files`() {
        listOf("<html>fake PNG</html>".toByteArray() to 415, image().copyOf(24) to 422, byteArrayOf(-1, -40, -1, 0) to 422).forEach { (bytes, status) ->
            upload(bytes).andExpect { status { isEqualTo(status) }; content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) } }
        }
        assertThat(references.list(projectId)).isEmpty()
        Files.list(directory.resolve("media").resolve(projectId.toString())).use { assertThat(it.count()).isZero() }
        assertThat(calls).isZero()
    }

    @Test
    fun `reference byte dimension and pixel limits are enforced`() {
        upload(image().copyOf(10 * 1024 * 1024 + 1)).andExpect { status { isEqualTo(413) } }
        upload(image(8193, 1)).andExpect { status { isEqualTo(422) } }
        upload(image(4001, 4000)).andExpect { status { isEqualTo(422) } }
        assertThat(references.list(projectId)).isEmpty()
    }

    @Test
    fun `reference metadata bounds and missing inputs are rejected`() {
        listOf(" " to "", "a".repeat(121) to "", "Valid" to "a".repeat(2001)).forEach { (title, guidance) -> upload(title = title, guidance = guidance).andExpect { status { isBadRequest() } } }
        mvc.multipart("/api/v1/projects/$projectId/references") { param("title", "Missing image"); param("guidance", "") }.andExpect { status { isBadRequest() } }
        upload(project = UUID.randomUUID()).andExpect { status { isNotFound() } }
        assertThat(calls).isZero()
    }

    @Test
    fun `all reference operations isolate projects`() {
        val saved = reference()
        val foreign = project()
        val path = "/api/v1/projects/$foreign/references/${saved.id}"
        mvc.get(path).andExpect { status { isNotFound() } }
        mvc.get("$path/content").andExpect { status { isNotFound() } }
        mvc.put(path) { contentType = MediaType.APPLICATION_JSON; content = """{"title":"foreign","guidance":""}""" }.andExpect { status { isNotFound() } }
        mvc.post("$path/archive").andExpect { status { isNotFound() } }
        assertThat(references.list(foreign)).isEmpty()
        assertThat(references.get(projectId, saved.id)).isEqualTo(saved)
    }

    @Test
    fun `metadata edit and idempotent archive preserve immutable image`() {
        val saved = reference()
        val path = "/api/v1/projects/$projectId/references/${saved.id}"
        mvc.put(path) { contentType = MediaType.APPLICATION_JSON; content = """{"title":" New title ","guidance":" Updated guidance "}""" }.andExpect { status { isOk() }; jsonPath("$.title") { value("New title") }; jsonPath("$.sha256") { value(saved.sha256) } }
        val archived = mvc.post("$path/archive").andExpect { status { isOk() }; jsonPath("$.archivedAt") { exists() } }.andReturn().response.contentAsString
        mvc.post("$path/archive").andExpect { content { json(archived) } }
        assertThat(references.list(projectId)).isEmpty()
        mvc.get(saved.url).andExpect { status { isOk() } }
        mvc.put(path) { contentType = MediaType.APPLICATION_JSON; content = """{"title":"No","guidance":""}""" }.andExpect { status { isConflict() } }
        assertThatThrownBy { jdbc.update("UPDATE visual_references SET archived_at = NULL WHERE id = ?", saved.id) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
        assertThatThrownBy { jdbc.update("UPDATE visual_references SET sha256 = ? WHERE id = ?", "a".repeat(64), saved.id) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
        assertThat(calls).isZero()
    }

    @Test
    fun `eight active references are deterministic and archive frees capacity`() {
        val saved = List(8) { reference() }
        upload().andExpect { status { isConflict() } }
        assertThat(references.list(projectId).map { it.id }).containsExactlyElementsOf(saved.map { it.id })
        references.archive(projectId, saved.first().id)
        val replacement = reference()
        assertThat(replacement.id).isNotEqualTo(saved.first().id)
        assertThat(references.list(projectId)).hasSize(8)
    }

    @Test
    fun `lifetime reference limit includes archived images`() {
        val saved = reference()
        references.archive(projectId, saved.id)
        repeat(99) { jdbc.update("INSERT INTO visual_references (id, project_id, title, guidance, width, height, sha256, archived_at) VALUES (?, ?, 'Capacity fixture', '', 1, 1, ?, CURRENT_TIMESTAMP)", UUID.randomUUID(), projectId, "a".repeat(64)) }
        upload().andExpect { status { isConflict() }; jsonPath("$.type") { value("urn:sceneproof:problem:reference-limit") } }
    }

    @Test
    fun `rules trim persist allow empty and reject invalid bounds without inference`() {
        val path = "/api/v1/projects/$projectId/rules"
        mvc.put(path) { contentType = MediaType.APPLICATION_JSON; content = """{"rules":"  New rule  "}""" }.andExpect { status { isOk() }; jsonPath("$.rules") { value("New rule") } }
        mvc.get("/api/v1/projects/$projectId").andExpect { jsonPath("$.rules") { value("New rule") } }
        mvc.put(path) { contentType = MediaType.APPLICATION_JSON; content = mapper.writeValueAsString(mapOf("rules" to "x".repeat(8001))) }.andExpect { status { isBadRequest() } }
        mvc.put(path) { contentType = MediaType.APPLICATION_JSON; content = """{"rules":" "}""" }.andExpect { status { isOk() }; jsonPath("$.rules") { value("") } }
        mvc.put("/api/v1/projects/${UUID.randomUUID()}/rules") { contentType = MediaType.APPLICATION_JSON; content = """{"rules":""}""" }.andExpect { status { isNotFound() } }
        assertThat(calls).isZero()
    }

    @Test
    fun `active references enter context snapshot and citations survive edits archive and replay`() {
        val saved = reference()
        val excluded = reference()
        references.archive(projectId, excluded.id)
        shot()
        val requestId = UUID.randomUUID()
        val response = analyze(requestId).andExpect { status { isOk() }; jsonPath("$.status") { value("SUCCEEDED") } }.andReturn().response.contentAsString
        val runId = UUID.fromString(mapper.readTree(response)["id"].asString())
        assertThat(captured.references.map { it.id }).containsExactly(saved.id)
        assertThat(captured.references.single().png).isNotEmpty()
        val snapshot = jdbc.queryForObject("SELECT context::text FROM analysis_runs WHERE id = ?", String::class.java, runId)!!
        assertThat(snapshot).contains(saved.id.toString(), saved.sha256, saved.guidance).doesNotContain("base64", "original.bin", "00.png", "data:image", "storage", "media.root")
        val finding = analyses.findings(projectId, runId, 0).single()
        assertThat(finding.relevantReferenceIds).containsExactly(saved.id)
        references.update(projectId, saved.id, ReferenceMetadataRequest("New title", "Different guidance"))
        references.archive(projectId, saved.id)
        mvc.put("/api/v1/projects/$projectId/rules") { contentType = MediaType.APPLICATION_JSON; content = """{"rules":"Current blue rule"}""" }.andExpect { status { isOk() } }
        val history = references.findingReferences(projectId, finding.id).single()
        assertThat(history.title).isEqualTo(saved.title)
        assertThat(history.guidance).isEqualTo(saved.guidance)
        assertThat(history.archivedAt).isNotNull()
        mvc.get("/api/v1/projects/$projectId/findings/${finding.id}/references").andExpect { status { isOk() }; jsonPath("$[0].title") { value(saved.title) } }
        mvc.get("/api/v1/projects/${project()}/findings/${finding.id}/references").andExpect { status { isNotFound() } }
        assertThat(jdbc.queryForObject("SELECT context::text FROM analysis_runs WHERE id = ?", String::class.java, runId)).isEqualTo(snapshot)
        analyze(requestId).andExpect { content { json(response) } }
        assertThat(calls).isEqualTo(1)
        assertThatThrownBy { jdbc.update("DELETE FROM visual_references WHERE id = ?", saved.id) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
        assertThatThrownBy { jdbc.update("UPDATE analysis_references SET guidance = 'rewritten' WHERE analysis_run_id = ?", runId) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
    }

    @Test
    fun `targeted PR4 uses original cited image metadata rules and previous judgement`() {
        val original = reference()
        shot()
        analyze().andExpect { status { isOk() } }
        val finding = analyses.findings(projectId, null, 0).single()
        references.update(projectId, original.id, ReferenceMetadataRequest("Updated", "Changed"))
        references.archive(projectId, original.id)
        val replacement = reference()
        mvc.put("/api/v1/projects/$projectId/rules") { contentType = MediaType.APPLICATION_JSON; content = """{"rules":"New rules"}""" }.andExpect { status { isOk() } }
        repeat(2) {
            val request = CreateFindingActionRequest(UUID.randomUUID(), FindingActionType.INTENTIONAL_CHANGE, "The prop was repainted.", "Between the affected shots.", finding.affectedShotIds)
            val response = mvc.post("/api/v1/projects/$projectId/findings/${finding.id}/actions") { contentType = MediaType.APPLICATION_JSON; content = mapper.writeValueAsString(request) }.andExpect { status { isOk() }; jsonPath("$.result.outcome") { value("ISSUE_REMAINS") } }.andReturn().response.contentAsString
            val runId = UUID.fromString(mapper.readTree(response)["reanalysis"]["id"].asString())
            assertThat(targeted.sequence.references.map { reference -> reference.id }).containsExactly(original.id).doesNotContain(replacement.id)
            assertThat(targeted.sequence.references.single().guidance).isEqualTo(original.guidance)
            assertThat(targeted.sequence.references.single().sha256).isEqualTo(original.sha256)
            assertThat(targeted.originalRules).isEqualTo("Original red rule")
            assertThat(targeted.sequence.rules).isEqualTo("New rules")
            assertThat(targeted.previousJudgement != null).isEqualTo(it > 0)
            assertThat(jdbc.queryForObject("SELECT context->'references'->0->>'id' FROM analysis_runs WHERE id = ?", String::class.java, runId)).isEqualTo(original.id.toString())
        }
        assertThat(analyses.finding(projectId, finding.id).relevantReferenceIds).containsExactly(original.id)
    }

    @Test
    fun `missing or corrupted reference fails closed before provider and yields durable safe failure`() {
        val reference = reference()
        shot()
        val file = storage.file(projectId, reference.id, "00.png")
        Files.write(file, image(64, 64))
        mvc.get(reference.url).andExpect { status { isServiceUnavailable() } }
        analyze().andExpect { status { isServiceUnavailable() }; jsonPath("$.type") { value("urn:sceneproof:problem:reference-unavailable") }; jsonPath("$.analysisRunId") { exists() } }
        Files.delete(file)
        analyze().andExpect { status { isServiceUnavailable() } }
        assertThat(calls).isZero()
        assertThat(analyses.findings(projectId, null, 0)).isEmpty()
    }

    @Test
    fun `invented foreign and archived unsubmitted citations fail with no saved findings`() {
        val archived = reference()
        references.archive(projectId, archived.id)
        val foreign = mapper.readValue(upload(project = project()).andReturn().response.contentAsString, ReferenceView::class.java)
        shot()
        listOf(UUID.randomUUID(), archived.id, foreign.id).forEach { id ->
            citations = listOf(id)
            analyze().andExpect { status { isBadGateway() }; jsonPath("$.type") { value("urn:sceneproof:problem:invalid-model-output") } }
        }
        assertThat(analyses.findings(projectId, null, 0)).isEmpty()
    }

    @Test
    fun `zero-reference and reference-free findings remain valid`() {
        shot()
        analyze().andExpect { status { isOk() } }
        assertThat(captured.references).isEmpty()
        val first = analyses.findings(projectId, null, 0).single()
        assertThat(references.findingReferences(projectId, first.id)).isEmpty()
        reference()
        citations = emptyList()
        analyze().andExpect { status { isOk() } }
        assertThat(captured.references).hasSize(1)
        assertThat(analyses.findings(projectId, null, 0).all { it.relevantReferenceIds.isEmpty() }).isTrue()
    }

    @Test
    fun `relational citation requires same project and submitted run`() {
        val submitted = reference()
        shot()
        analyze().andExpect { status { isOk() } }
        val finding = analyses.findings(projectId, null, 0).single()
        val unsubmitted = reference()
        val foreign = mapper.readValue(upload(project = project()).andReturn().response.contentAsString, ReferenceView::class.java)
        listOf(unsubmitted.id, foreign.id).forEach { id ->
            assertThatThrownBy { jdbc.update("INSERT INTO finding_references (finding_id, analysis_run_id, reference_id, project_id) VALUES (?, ?, ?, ?)", finding.id, finding.analysisRunId, id, projectId) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
        }
        assertThatThrownBy { jdbc.update("INSERT INTO finding_references (finding_id, analysis_run_id, reference_id, project_id) VALUES (?, ?, ?, ?)", finding.id, finding.analysisRunId, submitted.id, foreign.projectId) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
        assertThatThrownBy { jdbc.update("DELETE FROM finding_references WHERE finding_id = ?", finding.id) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
    }
}
