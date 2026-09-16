package dev.sceneproof

import dev.sceneproof.analysis.*
import dev.sceneproof.film.*
import dev.sceneproof.media.MediaRepository
import dev.sceneproof.project.CreateProjectRequest
import dev.sceneproof.project.ProjectService
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.mockito.Mockito.verifyNoInteractions
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest(properties = ["sceneproof.paid-runs.enabled=false"])
@AutoConfigureMockMvc
@ActiveProfiles("test", "paid-test")
class PaidRunsDisabledApiTest @Autowired constructor(
    private val mvc: MockMvc, private val jdbc: JdbcTemplate, private val projects: ProjectService,
    private val analyses: AnalysisRepository, private val films: FilmRepository,
    private val media: MediaRepository, private val mapper: ObjectMapper, private val manager: PlatformTransactionManager,
) {
    @MockitoBean lateinit var continuity: ContinuityAnalysisPort
    @MockitoBean lateinit var understanding: FilmUnderstandingPort
    @MockitoBean lateinit var transcription: AudioTranscriptionPort

    @Test fun `configured kill switch blocks new HTTP runs but permits persisted reads and request replay`() {
        check(jdbc.queryForObject("SELECT current_schema()", String::class.java) == "sceneproof_paid_test")
        jdbc.execute("TRUNCATE projects CASCADE")
        jdbc.execute("TRUNCATE paid_run_reservations")
        val project = projects.create(CreateProjectRequest("Disabled fixture"))
        val request = UUID.randomUUID()
        val prior = TransactionTemplate(manager).execute {
            AnalysisRepository(jdbc, mapper, media, PaidRunAdmission(jdbc, true, 2, 3)).start(project.id, request).first
        }!!
        analyses.fail(prior.id, AnalysisFailure("TEST_FAILURE", "Saved deterministic failure"))
        val source = films.saveSource(project.id, UUID.randomUUID(), "Fixture", "c".repeat(64), 12, 1000)
        mvc.post("/api/v1/projects/${project.id}/analyses") {
            contentType = MediaType.APPLICATION_JSON; content = """{"requestId":"${UUID.randomUUID()}"}"""
        }.andExpect { status { isTooManyRequests() }; jsonPath("$.type") { value("urn:sceneproof:problem:paid-runs-disabled") } }
        mvc.post("/api/v1/projects/${project.id}/film/runs") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"requestId":"${UUID.randomUUID()}","sourceFilmId":"${source.id}","paidConsent":true}"""
        }.andExpect { status { isTooManyRequests() } }
        mvc.get("/api/v1/projects/${project.id}/analyses/${prior.id}").andExpect { status { isOk() } }
        mvc.post("/api/v1/projects/${project.id}/analyses") {
            contentType = MediaType.APPLICATION_JSON; content = """{"requestId":"$request"}"""
        }.andExpect { status { isOk() }; jsonPath("$.id") { value(prior.id.toString()) } }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM paid_run_reservations", Long::class.java)).isEqualTo(1)
        verifyNoInteractions(continuity, understanding, transcription)
    }
}
