package dev.sceneproof

import dev.sceneproof.analysis.*
import dev.sceneproof.film.*
import dev.sceneproof.project.CreateProjectRequest
import dev.sceneproof.project.ProjectService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.delete
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.mockito.Mockito.verifyNoInteractions
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test", "paid-test")
class PaidRunAdmissionTest @Autowired constructor(
    private val jdbc: JdbcTemplate, private val admission: PaidRunAdmission,
    private val manager: PlatformTransactionManager, private val mvc: MockMvc,
    private val projects: ProjectService, private val analyses: AnalysisRepository, private val films: FilmRepository,
    private val media: dev.sceneproof.media.MediaRepository,
) {
    @MockitoBean lateinit var continuity: ContinuityAnalysisPort
    @MockitoBean lateinit var understanding: FilmUnderstandingPort
    @MockitoBean lateinit var transcription: AudioTranscriptionPort
    private val transaction get() = TransactionTemplate(manager)
    companion object {
        @org.junit.jupiter.api.io.TempDir @JvmStatic lateinit var directory: java.nio.file.Path
        @org.springframework.test.context.DynamicPropertySource @JvmStatic fun properties(registry: org.springframework.test.context.DynamicPropertyRegistry) {
            registry.add("sceneproof.media.root") { directory.resolve("media").toString() }
        }
    }

    @BeforeEach fun clean() {
        check(jdbc.queryForObject("SELECT current_schema()", String::class.java) == "sceneproof_paid_test")
        jdbc.execute("TRUNCATE projects CASCADE")
        jdbc.execute("TRUNCATE paid_run_reservations, project_deletions")
    }

    private fun reserve(id: UUID = UUID.randomUUID()) = transaction.executeWithoutResult { admission.reserve(id) }
    private fun count() = jdbc.queryForObject("SELECT count(*) FROM paid_run_reservations", Long::class.java)!!
    private fun exhausted(block: () -> Unit) {
        assertThatThrownBy(block).isInstanceOfSatisfying(AnalysisFailure::class.java) {
            assertThat(it.code).isEqualTo("PAID_RUN_LIMIT")
            assertThat(it.httpStatus).isEqualTo(429)
        }
    }

    @Test fun `reservation commits atomically and replay survives a new admission instance`() {
        val id = UUID.randomUUID()
        reserve(id)
        transaction.executeWithoutResult { PaidRunAdmission(jdbc, false, 2, 3).reserve(id) }
        assertThat(count()).isEqualTo(1)
        assertThatThrownBy {
            transaction.executeWithoutResult { admission.reserve(UUID.randomUUID()); error("rollback fixture") }
        }.isInstanceOf(IllegalStateException::class.java)
        assertThat(count()).isEqualTo(1)
        reserve()
        exhausted { transaction.executeWithoutResult { PaidRunAdmission(jdbc, true, 2, 3).reserve(UUID.randomUUID()) } }
    }

    @Test fun `hourly and daily windows count durable reservations and expire independently`() {
        reserve()
        reserve()
        exhausted { reserve() }
        jdbc.update("UPDATE paid_run_reservations SET reserved_at = clock_timestamp() - INTERVAL '61 minutes'")
        reserve()
        exhausted { reserve() }
        jdbc.update("UPDATE paid_run_reservations SET reserved_at = clock_timestamp() - INTERVAL '25 hours'")
        reserve()
        assertThat(count()).isEqualTo(1)
    }

    @Test fun `pruning reservation rows does not remove durable request replay identity`() {
        val project = projects.create(CreateProjectRequest("Pruned fixture"))
        val request = UUID.randomUUID()
        val run = analyses.start(project.id, request).first
        analyses.fail(run.id, AnalysisFailure("TEST_FAILURE", "Saved fixture"))
        jdbc.update("UPDATE paid_run_reservations SET reserved_at = clock_timestamp() - INTERVAL '25 hours'")
        reserve()
        assertThat(jdbc.queryForObject("SELECT count(*) FROM paid_run_reservations WHERE run_id = ?", Long::class.java, run.id)).isZero()
        reserve()
        mvc.post("/api/v1/projects/${project.id}/analyses") {
            contentType = MediaType.APPLICATION_JSON; content = """{"requestId":"$request"}"""
        }.andExpect { status { isOk() }; jsonPath("$.id") { value(run.id.toString()) } }
        assertThat(count()).isEqualTo(2)
        verifyNoInteractions(continuity, understanding, transcription)
    }

    @Test fun `run creation and its reservation roll back together`() {
        val project = projects.create(CreateProjectRequest("Rollback fixture"))
        assertThatThrownBy {
            transaction.executeWithoutResult {
                analyses.start(project.id, UUID.randomUUID())
                error("Fixture failure before commit")
            }
        }.isInstanceOf(IllegalStateException::class.java)
        assertThat(count()).isZero()
        assertThat(jdbc.queryForObject("SELECT count(*) FROM analysis_runs WHERE project_id = ?", Long::class.java, project.id)).isZero()
        verifyNoInteractions(continuity, understanding, transcription)
    }

    @Test fun `concurrent independent transactions cannot exceed the hourly ceiling`() {
        val ready = CountDownLatch(4)
        val start = CountDownLatch(1)
        Executors.newFixedThreadPool(4).use { executor ->
            val attempts = (0 until 4).map {
                executor.submit<Boolean> {
                    ready.countDown()
                    check(start.await(10, TimeUnit.SECONDS))
                    try { reserve(); true } catch (failure: AnalysisFailure) {
                        check(failure.code == "PAID_RUN_LIMIT")
                        false
                    }
                }
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue()
            start.countDown()
            assertThat(attempts.count { it.get(15, TimeUnit.SECONDS) }).isEqualTo(2)
        }
        assertThat(count()).isEqualTo(2)
    }

    @Test fun `film and continuity consume one each and replay consumes nothing`() {
        val project = projects.create(CreateProjectRequest("Admission fixture"))
        val sequenceRequest = UUID.randomUUID()
        val sequence = analyses.start(project.id, sequenceRequest).first
        analyses.fail(sequence.id, AnalysisFailure("TEST_FAILURE", "Deterministic failure"))
        assertThat(analyses.start(project.id, sequenceRequest).second).isFalse()
        val source = films.saveSource(project.id, UUID.randomUUID(), "Fixture", "a".repeat(64), 12, 1000)
        val filmRequest = UUID.randomUUID()
        val film = films.start(project.id, source.id, filmRequest).first
        films.fail(film.id, AnalysisFailure("TEST_FAILURE", "Deterministic failure"))
        assertThat(films.start(project.id, source.id, filmRequest).second).isFalse()
        assertThat(count()).isEqualTo(2)
        mvc.get("/api/v1/projects/${project.id}/analyses/${sequence.id}").andExpect { status { isOk() } }
        mvc.get("/api/v1/projects/${project.id}/film/runs/${film.id}").andExpect { status { isOk() } }
        mvc.delete("/api/v1/projects/${project.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"confirmationName":"Admission fixture"}"""
        }.andExpect { status { isNoContent() } }
        assertThat(count()).isEqualTo(2)
        exhausted { reserve() }
        verifyNoInteractions(continuity, understanding, transcription)
    }

    @Test fun `exhausted admission returns 429 for film and continuity before any provider invocation`() {
        val project = projects.create(CreateProjectRequest("Blocked fixture"))
        val source = films.saveSource(project.id, UUID.randomUUID(), "Fixture", "b".repeat(64), 12, 1000)
        reserve()
        reserve()
        mvc.post("/api/v1/projects/${project.id}/analyses") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"requestId":"${UUID.randomUUID()}"}"""
        }.andExpect { status { isTooManyRequests() }; header { doesNotExist("Retry-After") }; jsonPath("$.type") { value("urn:sceneproof:problem:paid-run-limit") } }
        mvc.post("/api/v1/projects/${project.id}/film/runs") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"requestId":"${UUID.randomUUID()}","sourceFilmId":"${source.id}","paidConsent":true}"""
        }.andExpect { status { isTooManyRequests() } }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM analysis_runs", Long::class.java)).isZero()
        assertThat(jdbc.queryForObject("SELECT count(*) FROM film_understanding_runs", Long::class.java)).isZero()
        assertThat(count()).isEqualTo(2)
        verifyNoInteractions(continuity, understanding, transcription)
    }

    @Test fun `kill switch rejects new reservations without disabling stored result reads`() {
        val project = projects.create(CreateProjectRequest("Saved fixture"))
        val request = UUID.randomUUID()
        val run = analyses.start(project.id, request).first
        analyses.fail(run.id, AnalysisFailure("TEST_FAILURE", "Deterministic failure"))
        val disabled = PaidRunAdmission(jdbc, false, 2, 3)
        assertThatThrownBy { transaction.executeWithoutResult { disabled.reserve(UUID.randomUUID()) } }
            .isInstanceOfSatisfying(AnalysisFailure::class.java) {
                assertThat(it.code).isEqualTo("PAID_RUNS_DISABLED")
                assertThat(it.httpStatus).isEqualTo(429)
            }
        transaction.executeWithoutResult { disabled.reserve(run.id) }
        mvc.get("/api/v1/projects/${project.id}/analyses/${run.id}").andExpect { status { isOk() } }
        assertThat(count()).isEqualTo(1)
        verifyNoInteractions(continuity, understanding, transcription)
    }

    @Test fun `targeted admission shares the ceiling and rejected action never reaches provider`() {
        val project = projects.create(CreateProjectRequest("Targeted fixture"))
        val shot = media.save(project.id, UUID.randomUUID(), "Fixture", dev.sceneproof.media.PreparedMedia("IMAGE", null, listOf(dev.sceneproof.media.PreparedFrame("00.png", null, 1, 1))), null)
        val frame = shot.frames.single()
        val context = AnalysisContext(project.id, project.name, "", "", listOf(AnalysisShot(shot.id, 0, shot.name, "IMAGE", null, 1, listOf(AnalysisFrame(frame.id, 0, null, 1, 1, byteArrayOf(1))))), emptyList())
        val run = analyses.start(project.id, UUID.randomUUID()).first
        analyses.succeed(run.id, context, AnalysisCompletion(ContinuityAnalysisResult("Fixture", listOf(ContinuityFinding(FindingCategory.PROP, FindingSeverity.LOW, 0.5, "Fixture", "Fixture", "Red", "Blue", "Fixture", listOf(shot.id), listOf(frame.id), emptyList(), "Keep red")), listOf(InspectedShot(shot.id, listOf(frame.id))), emptyList(), AnalysisMetadata(project.id, "1", "SAMPLED_SEQUENCE")), ANALYSIS_MODEL, null, null, null))
        val finding = analyses.findings(project.id, run.id, 0).single()
        reserve()
        mvc.post("/api/v1/projects/${project.id}/findings/${finding.id}/actions") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"requestId":"${UUID.randomUUID()}","type":"INTENTIONAL_CHANGE","explanation":"Intentional fixture change","scope":"Fixture scene","affectedShotIds":["${shot.id}"]}"""
        }.andExpect { status { isTooManyRequests() }; jsonPath("$.type") { value("urn:sceneproof:problem:paid-run-limit") } }
        assertThat(count()).isEqualTo(2)
        assertThat(jdbc.queryForObject("SELECT count(*) FROM finding_actions", Long::class.java)).isZero()
        verifyNoInteractions(continuity, understanding, transcription)
    }
}
