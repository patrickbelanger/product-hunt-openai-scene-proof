package dev.sceneproof

import dev.sceneproof.analysis.*
import dev.sceneproof.media.MediaStorage
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
import tools.jackson.databind.ObjectMapper
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import javax.imageio.ImageIO

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisApiTest @Autowired constructor(private val mvc: MockMvc, private val mapper: ObjectMapper, private val jdbc: JdbcTemplate, private val repository: AnalysisRepository, private val storage: MediaStorage) {
    @MockitoBean lateinit var port: ContinuityAnalysisPort
    private val projects = mutableListOf<UUID>()
    private fun any(): AnalysisContext = org.mockito.ArgumentMatchers.any(AnalysisContext::class.java)
        ?: AnalysisContext(UUID(0, 0), "", "", "", emptyList(), emptyList())

    companion object {
        @TempDir @JvmStatic lateinit var directory: Path
        @DynamicPropertySource @JvmStatic fun properties(registry: DynamicPropertyRegistry) {
            registry.add("sceneproof.media.root") { directory.resolve("media").toString() }
        }
    }

    @BeforeEach
    fun stub() {
        `when`(port.analyze(any())).thenAnswer { invocation -> completion(invocation.getArgument(0)) }
    }

    @AfterEach
    fun cleanup() {
        projects.forEach { id ->
            jdbc.update("DELETE FROM finding_frames WHERE finding_id IN (SELECT id FROM findings WHERE project_id = ?)", id)
            jdbc.update("DELETE FROM finding_shots WHERE project_id = ?", id)
            jdbc.update("DELETE FROM findings WHERE project_id = ?", id)
            jdbc.update("DELETE FROM analysis_runs WHERE project_id = ?", id)
            jdbc.update("DELETE FROM frames WHERE shot_id IN (SELECT id FROM shots WHERE project_id = ?)", id)
            jdbc.update("DELETE FROM shots WHERE project_id = ?", id)
            jdbc.update("DELETE FROM projects WHERE id = ?", id)
        }
    }

    private fun project(): UUID {
        val response = mvc.post("/api/v1/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Analysis test original","rules":"The square stays red."}"""
        }.andExpect { status { isCreated() } }.andReturn().response
        return UUID.fromString(mapper.readTree(response.contentAsString)["id"].asString()).also { projects.add(it) }
    }

    private fun image(project: UUID) = mvc.multipart("/api/v1/projects/$project/shots") {
        val bytes = ByteArrayOutputStream().use { ImageIO.write(BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB), "png", it); it.toByteArray() }
        file(MockMultipartFile("file", "original.png", "image/png", bytes))
    }.andExpect { status { isCreated() } }.andReturn().response.let { mapper.readTree(it.contentAsString) }

    private fun analyze(project: UUID, request: UUID = UUID.randomUUID()) = mvc.post("/api/v1/projects/$project/analyses") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"requestId":"$request"}"""
    }

    private fun completion(context: AnalysisContext): AnalysisCompletion {
        val shots = context.shots.map { it.id }
        val frames = context.shots.flatMap { it.frames.map { frame -> frame.id } }
        return AnalysisCompletion(ContinuityAnalysisResult("Fixture result", listOf(ContinuityFinding(FindingCategory.PROP, FindingSeverity.HIGH, 0.9, "Color drift", "Color changed", "Red", "Blue", "The rule requires red", shots, frames, emptyList(), "Keep the square red")), context.shots.map { InspectedShot(it.id, it.frames.map { frame -> frame.id }) }, emptyList(), AnalysisMetadata(context.projectId, "1", "SAMPLED_SEQUENCE")), "gpt-6-astra", "resp_fixture", "req_fixture", AnalysisUsage(73, 20, 93, 0, 0, 0))
    }

    @Test
    fun `success persists run findings evidence usage and reuses request id`() {
        val project = project()
        val shot = image(project)
        val requestId = UUID.randomUUID()
        val response = analyze(project, requestId).andExpect {
            status { isOk() }; jsonPath("$.status") { value("SUCCEEDED") }; jsonPath("$.usage.totalTokens") { value(93) }
            jsonPath("$.shotCount") { value(1) }; jsonPath("$.frameCount") { value(1) }
        }.andReturn().response
        val runId = mapper.readTree(response.contentAsString)["id"].asString()
        mvc.get(response.getHeader("Location")!!).andExpect { content { json(response.contentAsString) } }
        analyze(project, requestId).andExpect { content { json(response.contentAsString) } }
        verify(port, times(1)).analyze(any())
        mvc.get("/api/v1/projects/$project/findings?analysisId=$runId").andExpect {
            status { isOk() }; jsonPath("$[0].analysisRunId") { value(runId) }; jsonPath("$[0].status") { value("OPEN") }
            jsonPath("$[0].category") { value("PROP") }; jsonPath("$[0].affectedShotIds[0]") { value(shot["id"].asString()) }
            jsonPath("$[0].relevantFrameIds[0]") { value(shot["frames"][0]["id"].asString()) }
            jsonPath("$[0].suggestedCorrectionPrompt") { value("Keep the square red") }
        }
        val snapshot = jdbc.queryForObject("SELECT context::text FROM analysis_runs WHERE id = ?", String::class.java, UUID.fromString(runId))!!
        assertThat(snapshot).contains("sha256", "The square stays red.").doesNotContain("base64", "data:image", "storage_key")
    }

    @Test
    fun `no shots missing frame failed imports and oversized sequence avoid provider`() {
        val empty = project()
        analyze(empty).andExpect { status { isUnprocessableEntity() }; jsonPath("$.analysisRunId") { exists() } }
        val missing = project()
        val shot = image(missing)
        Files.delete(storage.file(missing, UUID.fromString(shot["id"].asString()), "00.png"))
        analyze(missing).andExpect { status { isServiceUnavailable() } }
        val tooMany = project()
        repeat(9) { image(tooMany) }
        analyze(tooMany).andExpect { status { isUnprocessableEntity() } }
        mvc.multipart("/api/v1/projects/$empty/shots") { file(MockMultipartFile("file", "invalid.png", "image/png", byteArrayOf(1))) }
        analyze(empty).andExpect { status { isUnprocessableEntity() } }
        verifyNoInteractions(port)
        assertThat(jdbc.queryForObject("SELECT count(*) FROM analysis_runs WHERE project_id = ? AND status = 'FAILED'", Long::class.java, empty)).isEqualTo(2)
    }

    @Test
    fun `provider failure is durable safe and never automatically retried`() {
        val project = project()
        image(project)
        `when`(port.analyze(any())).thenThrow(AnalysisFailure("PROVIDER_RATE_LIMITED", "OpenAI limits reached.", 429, AnalysisUsage(4, 0, 4, null, null)))
        val request = UUID.randomUUID()
        val failure = analyze(project, request).andExpect { status { isTooManyRequests() }; jsonPath("$.analysisRunId") { exists() } }.andReturn().response
        val runId = mapper.readTree(failure.contentAsString)["analysisRunId"].asString()
        mvc.get("/api/v1/projects/$project/analyses/$runId").andExpect { jsonPath("$.status") { value("FAILED") }; jsonPath("$.failureCode") { value("PROVIDER_RATE_LIMITED") }; jsonPath("$.usage.inputTokens") { value(4) } }
        analyze(project, request).andExpect { status { isOk() }; jsonPath("$.status") { value("FAILED") } }
        mvc.get("/api/v1/projects/$project/findings").andExpect { content { json("[]") } }
        verify(port, times(1)).analyze(any())
    }

    @Test
    fun `foreign output identifiers fail before any finding persistence`() {
        val project = project()
        image(project)
        `when`(port.analyze(any())).thenAnswer { invocation ->
            val answer = completion(invocation.getArgument(0))
            answer.copy(result = answer.result.copy(findings = listOf(answer.result.findings.single().copy(relevantFrameIds = listOf(UUID.randomUUID())))))
        }
        analyze(project).andExpect { status { isBadGateway() }; jsonPath("$.type") { value("urn:sceneproof:problem:invalid-model-output") } }
        assertThat(repository.findings(project, null, 0)).isEmpty()
        assertThat(jdbc.queryForObject("SELECT status FROM analysis_runs WHERE project_id = ?", String::class.java, project)).isEqualTo("FAILED")
        assertThat(jdbc.queryForObject("SELECT usage->>'totalTokens' FROM analysis_runs WHERE project_id = ?", String::class.java, project)).isEqualTo("93")
    }

    @Test
    fun `API rejects cross project analysis lookups invalid bodies and pagination`() {
        val first = project()
        val second = project()
        val (run) = repository.start(first, UUID.randomUUID())
        mvc.get("/api/v1/projects/$second/analyses/${run.id}").andExpect { status { isNotFound() } }
        mvc.get("/api/v1/projects/$second/findings?analysisId=${run.id}").andExpect { status { isNotFound() } }
        mvc.get("/api/v1/projects/$first/findings?page=-1").andExpect { status { isBadRequest() } }
        listOf("{}", """{"requestId":"invalid"}""", """{"requestId":"${UUID.randomUUID()}","extra":true}""").forEach { body ->
            mvc.post("/api/v1/projects/$first/analyses") { contentType = MediaType.APPLICATION_JSON; content = body }.andExpect { status { isBadRequest() } }
        }
        verifyNoInteractions(port)
    }

    @Test
    fun `running requests are deduplicated other work is bounded and interrupted work expires`() {
        val project = project()
        val request = UUID.randomUUID()
        val (run) = repository.start(project, request)
        analyze(project, request).andExpect { status { isOk() }; jsonPath("$.status") { value("RUNNING") } }
        analyze(project).andExpect { status { isTooManyRequests() } }
        jdbc.update("UPDATE analysis_runs SET started_at = CURRENT_TIMESTAMP - INTERVAL '6 minutes' WHERE id = ?", run.id)
        mvc.get("/api/v1/projects/$project/analyses/${run.id}").andExpect { jsonPath("$.status") { value("FAILED") }; jsonPath("$.failureCode") { value("ANALYSIS_INTERRUPTED") } }
        verifyNoInteractions(port)
    }

    @Test
    fun `finding insertion failure rolls back succeeded state and partial findings`() {
        val project = project()
        val shot = image(project)
        val context = AnalysisContext(project, "Fixture", "", "", listOf(AnalysisShot(UUID.fromString(shot["id"].asString()), 0, "Fixture", "IMAGE", null, 1, listOf(AnalysisFrame(UUID.fromString(shot["frames"][0]["id"].asString()), 0, null, 128, 128, byteArrayOf())))), emptyList())
        val (run) = repository.start(project, UUID.randomUUID())
        val answer = completion(context)
        val invalid = answer.copy(result = answer.result.copy(findings = listOf(answer.result.findings.single(), answer.result.findings.single().copy(affectedShotIds = listOf(UUID.randomUUID())))))
        assertThatThrownBy { repository.succeed(run.id, context, invalid) }.isInstanceOf(org.springframework.dao.DataAccessException::class.java)
        assertThat(repository.get(project, run.id).status).isEqualTo("RUNNING")
        assertThat(jdbc.queryForObject("SELECT count(*) FROM findings WHERE analysis_run_id = ?", Long::class.java, run.id)).isZero()
        repository.fail(run.id, AnalysisFailure("ANALYSIS_PERSISTENCE_FAILURE", "Persistence failed.", 503))
        assertThat(repository.get(project, run.id).status).isEqualTo("FAILED")
    }
}
