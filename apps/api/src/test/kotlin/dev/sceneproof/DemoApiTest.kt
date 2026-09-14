package dev.sceneproof

import dev.sceneproof.analysis.*
import dev.sceneproof.demo.*
import dev.sceneproof.media.ImageContent
import dev.sceneproof.media.MediaRepository
import dev.sceneproof.media.MediaStorage
import dev.sceneproof.project.CreateProjectRequest
import dev.sceneproof.project.ProjectService
import dev.sceneproof.project.ProjectView
import dev.sceneproof.project.UpdateRulesRequest
import dev.sceneproof.reference.ReferenceMetadataRequest
import dev.sceneproof.reference.ReferenceRepository
import org.assertj.core.api.Assertions.assertThat
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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "demo-test")
class DemoApiTest @Autowired constructor(
    private val mvc: MockMvc, private val mapper: ObjectMapper, private val jdbc: JdbcTemplate,
    private val demos: DemoService, private val projects: ProjectService,
    private val references: ReferenceRepository, private val media: MediaRepository,
    private val assembler: AnalysisContextAssembler, private val storage: MediaStorage,
) {
    @MockitoBean lateinit var source: DemoTemplateSource
    @MockitoBean lateinit var port: ContinuityAnalysisPort
    private val bytes = ByteArrayOutputStream().use { output ->
        ImageIO.write(BufferedImage(64, 96, BufferedImage.TYPE_INT_RGB), "png", output)
        output.toByteArray()
    }
    private val reference = DemoAsset("reference-01.png", ImageContent.sha256(bytes), "Fixture reference", "Authored visual context")
    private val first = DemoAsset("shot-01.png", ImageContent.sha256(bytes), "First fixture shot")
    private val second = DemoAsset("shot-02.png", ImageContent.sha256(bytes), "Second fixture shot")
    private val template = DemoTemplate("test-only-v1", CreateProjectRequest("Test-only demo", "Generated test fixtures", "Authored rules"), listOf(reference), listOf(first, second))

    companion object {
        @TempDir @JvmStatic lateinit var directory: Path
        @DynamicPropertySource @JvmStatic fun properties(registry: DynamicPropertyRegistry) {
            registry.add("sceneproof.media.root") { directory.resolve("media").toString() }
        }
    }

    @BeforeEach fun setup() {
        cleanup()
        check(jdbc.queryForObject("SELECT current_schema()", String::class.java) == "sceneproof_demo_test")
        `when`(source.template()).thenReturn(template)
        (template.references + template.shots).forEach { asset -> `when`(source.upload(asset)).thenReturn(DemoUpload(asset.title, bytes)) }
    }

    @AfterEach fun cleanup() {
        check(jdbc.queryForObject("SELECT current_schema()", String::class.java) == "sceneproof_demo_test")
        jdbc.execute("TRUNCATE film_candidates, film_transcript_segments, film_segments, film_understanding_stages, film_understanding_runs, source_films, demo_replacements, finding_references, analysis_references, visual_references, targeted_results, finding_action_shots, finding_actions, finding_frames, finding_shots, findings, analysis_runs, frames, shots, projects")
    }

    private fun open(request: UUID = UUID.randomUUID()) = mvc.post("/api/v1/demo") {
        contentType = MediaType.APPLICATION_JSON; content = """{"requestId":"$request"}"""
    }
    private fun created(request: UUID = UUID.randomUUID()): ProjectView = mapper.readValue(open(request).andExpect { status { isOk() } }.andReturn().response.contentAsString, ProjectView::class.java)
    private fun reset(project: UUID) = mvc.post("/api/v1/projects/$project/demo/reset")
    private fun count() = jdbc.queryForObject("SELECT count(*) FROM projects", Long::class.java)

    @Test fun `create publishes populated real project rules references ordered frames and content with explicit identity`() {
        val project = created()
        assertThat(project.rules).isEqualTo(template.project.rules)
        assertThat(project.demo!!.templateVersion).isEqualTo(template.version)
        assertThat(project.demo.retired).isFalse()
        val bible = references.list(project.id)
        assertThat(bible.map { it.title }).containsExactly(reference.title)
        val shots = media.list(project.id)
        assertThat(shots.map { it.position }).containsExactly(0, 1)
        assertThat(shots.map { it.name }).containsExactly(first.title, second.title)
        assertThat(shots.all { it.status == "READY" && it.frames.size == 1 }).isTrue()
        (shots.flatMap { it.frames.map { frame -> frame.url } } + bible.map { it.url }).forEach { url ->
            val content = mvc.get(url).andExpect { status { isOk() }; content { contentType(MediaType.IMAGE_PNG) } }.andReturn().response.contentAsByteArray
            assertThat(ImageIO.read(content.inputStream()).width).isEqualTo(64)
        }
        mvc.get("/api/v1/projects/${project.id}/findings").andExpect { status { isOk() }; content { json("[]") } }
        verifyNoInteractions(port)
    }

    @Test fun `repeated and concurrent launch recover one current copy`() {
        val request = UUID.randomUUID()
        Executors.newFixedThreadPool(2).use { executor ->
            val calls = (1..2).map { executor.submit<ProjectView> { demos.open(request) } }
            val copies = calls.map { it.get(30, TimeUnit.SECONDS) }
            assertThat(copies.map { it.id }.distinct()).hasSize(1)
            assertThat(created(request).id).isEqualTo(copies.first().id)
        }
        assertThat(count()).isEqualTo(1)
    }

    @Test fun `separate request identities have independent media ownership`() {
        val firstCopy = created(); val other = created()
        assertThat(firstCopy.demo!!.instanceId).isNotEqualTo(other.demo!!.instanceId)
        val frame = media.list(firstCopy.id).first().frames.first()
        mvc.get("/api/v1/projects/${other.id}/frames/${frame.id}/content").andExpect { status { isNotFound() } }
        val visual = references.list(firstCopy.id).first()
        mvc.get("/api/v1/projects/${other.id}/references/${visual.id}/content").andExpect { status { isNotFound() } }
    }

    @Test fun `failed seed rolls back publication and same request can retry`() {
        val request = UUID.randomUUID()
        `when`(source.upload(second)).thenReturn(DemoUpload(second.title, byteArrayOf(1)))
        open(request).andExpect { status { isUnsupportedMediaType() } }
        assertThat(count()).isZero()
        assertThat(jdbc.queryForObject("SELECT count(*) FROM shots", Long::class.java)).isZero()
        `when`(source.upload(second)).thenReturn(DemoUpload(second.title, bytes))
        assertThat(created(request).demo!!.instanceId).isEqualTo(request)
        assertThat(count()).isEqualTo(1)
    }

    @Test fun `reset restores authored state while preserving immutable creator actions and other projects`() {
        val original = created(); val other = created()
        val ordinary = projects.create(CreateProjectRequest("Ordinary project", rules = "Ordinary rules"))
        projects.updateRules(original.id, UpdateRulesRequest("Creator edits"))
        val visual = references.list(original.id).single()
        references.update(original.id, visual.id, ReferenceMetadataRequest("Creator title", "Creator guidance"))
        references.archive(original.id, visual.id)
        mvc.multipart("/api/v1/projects/${original.id}/shots") {
            file(MockMultipartFile("file", "creator-upload.png", "image/png", bytes))
        }.andExpect { status { isCreated() } }
        val context = assembler.assemble(original.id)
        `when`(port.analyze(context)).thenReturn(completion(context))
        doAnswer { invocation -> completion(invocation.getArgument(0)) }.`when`(port).analyze(anyContext())
        val run = mvc.post("/api/v1/projects/${original.id}/analyses") { contentType = MediaType.APPLICATION_JSON; content = """{"requestId":"${UUID.randomUUID()}"}""" }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        val findings = mapper.readTree(mvc.get("/api/v1/projects/${original.id}/findings").andReturn().response.contentAsString)
        val finding = findings[0]
        mvc.post("/api/v1/projects/${original.id}/findings/${finding["id"].asString()}/actions") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(mapOf("requestId" to UUID.randomUUID(), "type" to "DISMISS", "explanation" to "Test creator decision", "scope" to "This test sequence", "affectedShotIds" to context.shots.map { it.id }))
        }.andExpect { status { isOk() } }
        val fresh = demos.reset(original.id)
        assertThat(fresh.id).isNotEqualTo(original.id)
        assertThat(fresh.rules).isEqualTo(template.project.rules)
        assertThat(references.list(fresh.id).map { it.title }).containsExactly(reference.title)
        assertThat(media.list(fresh.id).map { it.name }).containsExactly(first.title, second.title)
        assertThat(projects.get(original.id).demo!!.retired).isTrue()
        assertThat(projects.list(0).items.map { it.id }).doesNotContain(original.id).contains(fresh.id, other.id, ordinary.id)
        assertThat(projects.get(ordinary.id).rules).isEqualTo("Ordinary rules")
        assertThat(references.list(other.id)).hasSize(1)
        assertThat(jdbc.queryForObject("SELECT count(*) FROM finding_actions WHERE project_id = ?", Long::class.java, original.id)).isEqualTo(1)
        assertThat(jdbc.queryForObject("SELECT count(*) FROM analysis_runs WHERE project_id = ?", Long::class.java, fresh.id)).isZero()
        mvc.get("/api/v1/projects/${original.id}/analyses/${mapper.readTree(run)["id"].asString()}").andExpect { status { isOk() }; jsonPath("$.status") { value("SUCCEEDED") } }
    }

    @Test fun `reset ordinary or missing project is rejected without changes`() {
        val ordinary = projects.create(CreateProjectRequest("Ordinary"))
        reset(ordinary.id).andExpect { status { isConflict() } }
        reset(UUID.randomUUID()).andExpect { status { isNotFound() } }
        assertThat(count()).isEqualTo(1)
        verifyNoInteractions(source, port)
    }

    @Test fun `reset replay and landing recovery follow current copy even across later resets`() {
        val request = UUID.randomUUID(); val original = created(request)
        val fresh = demos.reset(original.id)
        assertThat(demos.reset(original.id).id).isEqualTo(fresh.id)
        val latest = demos.reset(fresh.id)
        assertThat(demos.reset(original.id).id).isEqualTo(latest.id)
        assertThat(created(request).id).isEqualTo(latest.id)
        assertThat(count()).isEqualTo(3)
    }

    @Test fun `failed reset leaves current modified copy intact and retry restores it`() {
        val original = created()
        projects.updateRules(original.id, UpdateRulesRequest("Keep until success"))
        `when`(source.upload(second)).thenThrow(IllegalStateException("Fixture failure"))
        reset(original.id).andExpect { status { isServiceUnavailable() } }
        assertThat(projects.get(original.id).demo!!.retired).isFalse()
        assertThat(projects.get(original.id).rules).isEqualTo("Keep until success")
        assertThat(count()).isEqualTo(1)
        doReturn(DemoUpload(second.title, bytes)).`when`(source).upload(second)
        assertThat(demos.reset(original.id).rules).isEqualTo(template.project.rules)
    }

    @Test fun `reset does not silently migrate an older template version`() {
        val original = created()
        `when`(source.template()).thenReturn(template.copy(version = "test-only-v2"))
        reset(original.id).andExpect { status { isConflict() } }
        assertThat(count()).isEqualTo(1)
    }

    @Test fun `evaluation fields and notes cannot enter assembled context or Astra request serialization`() {
        val project = created()
        val context = assembler.assemble(project.id)
        val adapter = AstraContinuityAnalysisAdapter(mapper, ContinuityResultValidator(mapper), "test-only-unused")
        val request = String(adapter.requestBody(context), Charsets.UTF_8)
        val serialized = mapper.writeValueAsString(context)
        val manifest = mapper.readTree(Files.readString(Path.of("../../demo/evaluation/expectations.json")))
        val evaluationKeys = listOf("expectedDisposition", "historicalCandidates", "templateVersion", "humanNotes", "demoInstanceId")
        evaluationKeys.forEach { assertThat(request).doesNotContain(it); assertThat(serialized).doesNotContain(it) }
        manifest["cases"].forEach { item -> assertThat(request).doesNotContain(item["question"].asString(), item["id"].asString()) }
        assertThat(request).contains(template.project.rules, reference.guidance)
        assertThat(javaClass.classLoader.getResource("demo/evaluation/expectations.json")).isNull()
        verifyNoInteractions(port)
    }

    @Test fun `malformed launch request fails before creating any project`() {
        mvc.post("/api/v1/demo") { contentType = MediaType.APPLICATION_JSON; content = """{"requestId":"not-a-uuid"}""" }.andExpect { status { isBadRequest() } }
        assertThat(count()).isZero()
    }

    private fun anyContext(): AnalysisContext = org.mockito.ArgumentMatchers.any(AnalysisContext::class.java) ?: AnalysisContext(UUID(0, 0), "", "", "", emptyList(), emptyList())
    @Test fun `packaged template has approved hashes and authored inputs without evaluation fields`() {
        val packaged = PackagedDemoTemplate(mapper)
        val actual = packaged.template()
        assertThat(actual.version).isEqualTo("between-the-line-v1")
        assertThat(actual.shots).hasSize(8)
        assertThat(actual.references).hasSize(5)
        assertThat(actual.sourceFilm!!.file).isEqualTo("analysis-source.mp4")
        assertThat(actual.sourceProvenance!!.masterSha256).isEqualTo("ac9b29c47eeb399dbd1ac5cdfcde19273e0e4e68f5df8fd67fedbef3d284a7a7")
        assertThat(actual.sourceProvenance.sourceEndUs).isEqualTo(36291667)
        val authored = mapper.readTree(Files.readString(Path.of("../../demo/curation.json")))
        assertThat(actual.project.rules).isEqualTo(authored["project"]["rules"].asString())
        assertThat(actual.shots.map { it.title }).containsExactlyElementsOf(authored["shots"].toList().map { it["title"].asString() })
        assertThat(actual.references.map { it.title }).containsExactlyElementsOf(authored["references"].toList().map { it["title"].asString() })
        assertThat(mapper.writeValueAsString(actual)).doesNotContain("expectedDisposition", "historicalCandidates", "humanNotes", "blazer")
        assertThat(javaClass.classLoader.getResource("demo/curation.json")).isNull()
        verifyNoInteractions(port)
    }
    private fun completion(context: AnalysisContext) = AnalysisCompletion(ContinuityAnalysisResult(
        "Test-only result", listOf(ContinuityFinding(FindingCategory.PROP, FindingSeverity.LOW, 0.7, "Test concern", "Test summary", "Test expected", "Test observed", "Test explanation", context.shots.map { it.id }, context.shots.flatMap { it.frames.map { frame -> frame.id } }, emptyList(), "Test correction")),
        context.shots.map { InspectedShot(it.id, it.frames.map { frame -> frame.id }) }, emptyList(), AnalysisMetadata(context.projectId, "1", "SAMPLED_SEQUENCE")), ANALYSIS_MODEL, null, null, null)
}
