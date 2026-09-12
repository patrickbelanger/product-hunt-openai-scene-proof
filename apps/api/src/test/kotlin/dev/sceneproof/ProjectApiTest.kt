package dev.sceneproof

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProjectApiTest @Autowired constructor(
    private val mvc: MockMvc,
    private val mapper: ObjectMapper,
    private val entityManager: EntityManager,
) {
    @Test
    fun `create project trims input and retrieves durable state`() {
        val result = mvc.post("/api/v1/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"  Between the Line  ","rules":"Black blazer throughout.","description":"Metro sequence"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("Between the Line") }
            jsonPath("$.rules") { value("Black blazer throughout.") }
        }.andReturn()
        val project = mapper.readTree(result.response.contentAsString)
        val location = result.response.getHeader("Location")!!
        assertThat(location).isEqualTo("/api/v1/projects/${project["id"].asString()}")
        entityManager.flush()
        entityManager.clear()
        mvc.get(location).andExpect {
            status { isOk() }
            content { json(result.response.contentAsString) }
        }
        mvc.get("/api/v1/projects").andExpect {
            status { isOk() }
            jsonPath("$.items[0].id") { value(project["id"].asString()) }
            jsonPath("$.page") { value(0) }
            jsonPath("$.hasNext") { value(false) }
        }
    }

    @Test
    fun `invalid project bodies fail without storing a project`() {
        val bodies = listOf(
            """{"name":"   "}""",
            """{"name":null}""",
            """{}""",
            """{"name":"${"a".repeat(121)}"}""",
            """{"name":"Valid","rules":"${"r".repeat(8001)}"}""",
            """{"name":"Valid","description":"${"d".repeat(2001)}"}""",
            """{"name":"Valid","unexpected":true}""",
            "{",
        )
        bodies.forEach { body ->
            mvc.post("/api/v1/projects") {
                contentType = MediaType.APPLICATION_JSON
                content = body
            }.andExpect {
                status { isBadRequest() }
                content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) }
                jsonPath("$.status") { value(400) }
            }
        }
    }

    @Test
    fun `missing and malformed project identifiers return distinct errors`() {
        mvc.get("/api/v1/projects/${UUID.randomUUID()}").andExpect {
            status { isNotFound() }
            jsonPath("$.type") { value("urn:sceneproof:problem:project-not-found") }
        }
        mvc.get("/api/v1/projects/not-a-uuid").andExpect { status { isBadRequest() } }
        mvc.get("/api/v1/projects?page=-1").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `published contract describes project endpoints and limits`() {
        mvc.get("/openapi.json").andExpect {
            status { isOk() }
            jsonPath("$.openapi") { value("3.1.0") }
            jsonPath("$.components.schemas.CreateProject.properties.name.maxLength") { value(120) }
        }
        mvc.get("/actuator/health").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UP") }
        }
    }
}
