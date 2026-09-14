package dev.sceneproof

import dev.sceneproof.media.MediaProcess
import dev.sceneproof.media.MediaFailure
import dev.sceneproof.media.LocalMediaStorage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
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
@Transactional
class MediaApiTest @Autowired constructor(private val mvc: MockMvc, private val mapper: ObjectMapper, private val entityManager: jakarta.persistence.EntityManager) {
    companion object {
        @TempDir @JvmStatic lateinit var directory: Path
        @DynamicPropertySource @JvmStatic fun properties(registry: DynamicPropertyRegistry) {
            registry.add("sceneproof.media.root") { directory.resolve("media").toString() }
        }
    }

    private fun project(): String = mapper.readTree(mvc.post("/api/v1/projects") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"name":"Media verification"}"""
    }.andExpect { status { isCreated() } }.andReturn().response.contentAsString)["id"].asString().also { entityManager.flush() }

    private fun image(width: Int = 40, height: Int = 30, format: String = "png"): ByteArray = ByteArrayOutputStream().use {
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, it)
        it.toByteArray()
    }

    @Test
    fun `image ingestion ignores client paths and mime and preserves project scoped frames`() {
        val project = project()
        val response = mvc.multipart("/api/v1/projects/$project/shots") {
            file(MockMultipartFile("file", "../../portrait.png", "text/html", image()))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("portrait.png") }
            jsonPath("$.status") { value("READY") }
            jsonPath("$.frames[0].timestampMs") { value(null) }
            jsonPath("$.frames[0].width") { value(40) }
        }.andReturn().response.contentAsString
        val shot = mapper.readTree(response)
        mvc.get("/api/v1/projects/$project/shots").andExpect { content { json("[$response]") } }
        val url = shot["frames"][0]["url"].asString()
        val content = mvc.get(url).andExpect {
            status { isOk() }
            content { contentType(MediaType.IMAGE_PNG) }
            header { string("X-Content-Type-Options", "nosniff") }
        }.andReturn().response.contentAsByteArray
        assertThat(ImageIO.read(content.inputStream()).width).isEqualTo(40)
        mvc.get(url.replace(project, project())).andExpect { status { isNotFound() } }
        mvc.multipart("/api/v1/projects/$project/shots") {
            file(MockMultipartFile("file", "second.jpg", "image/jpeg", image(2000, 1000, "jpeg")))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.position") { value(1) }
            jsonPath("$.frames[0].width") { value(1600) }
            jsonPath("$.frames[0].height") { value(800) }
        }
    }

    @Test
    fun `corrupt unsupported oversized images leave failed records and no files`() {
        val project = project()
        val oversized = image().copyOf(10 * 1024 * 1024 + 1)
        val cases = listOf("<script>alert(1)</script>".toByteArray() to 415, image().copyOf(24) to 422, oversized to 413, image(8193, 1) to 422)
        cases.forEach { (bytes, expectedStatus) ->
            mvc.multipart("/api/v1/projects/$project/shots") {
                file(MockMultipartFile("file", "fake.png", "image/png", bytes))
            }.andExpect { status { isEqualTo(expectedStatus) }; content { contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) } }
        }
        val shots = mapper.readTree(mvc.get("/api/v1/projects/$project/shots").andReturn().response.contentAsString)
        assertThat(shots.size()).isEqualTo(cases.size)
        shots.forEach { assertThat(it["status"].asString()).isEqualTo("FAILED"); assertThat(it["frames"].size()).isZero() }
        Files.list(directory.resolve("media").resolve(project)).use { assertThat(it.count()).isZero() }
    }

    @Test
    fun `unknown project and missing part do not import`() {
        mvc.multipart("/api/v1/projects/${UUID.randomUUID()}/shots") { file(MockMultipartFile("file", image())) }.andExpect { status { isNotFound() } }
        mvc.multipart("/api/v1/projects/${project()}/shots").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `real h264 scene cut yields ordered source timestamps and durable pixels`() {
        val fixture = directory.resolve("scene-cut.mp4")
        MediaProcess().run(listOf("ffmpeg", "-nostdin", "-v", "error", "-f", "lavfi", "-i", "color=red:s=160x90:r=12:d=1", "-f", "lavfi", "-i", "color=blue:s=160x90:r=12:d=1", "-filter_complex", "[0:v][1:v]concat=n=2:v=1:a=0", "-c:v", "libx264", "-pix_fmt", "yuv420p", fixture.toString()))
        val project = project()
        val response = mvc.multipart("/api/v1/projects/$project/shots") { file(MockMultipartFile("file", "cut.mp4", "video/mp4", Files.readAllBytes(fixture))) }.andExpect {
            status { isCreated() }
            jsonPath("$.kind") { value("VIDEO") }
            jsonPath("$.durationMs") { value(2000) }
            jsonPath("$.frames[0].width") { value(160) }
            jsonPath("$.frames[0].height") { value(90) }
        }.andReturn().response.contentAsString
        val frames = mapper.readTree(response)["frames"]
        val timestamps = frames.toList().map { it["timestampMs"].asLong() }
        assertThat(timestamps.size).isBetween(2, 32)
        assertThat(timestamps).isSorted().doesNotHaveDuplicates().contains(0L, 1000L)
        val first = mvc.get(frames.first()["url"].asString()).andReturn().response.contentAsByteArray
        val last = mvc.get(frames.last()["url"].asString()).andReturn().response.contentAsByteArray
        assertThat(ImageIO.read(first.inputStream()).getRGB(10, 10)).isNotEqualTo(ImageIO.read(last.inputStream()).getRGB(10, 10))
        mvc.get("/api/v1/projects/$project/shots").andExpect { content { json("[$response]") } }
    }

    @Test
    fun `h264 mp4 with iso6 major brand is confirmed by ffprobe and imported`() {
        val fixture = directory.resolve("iso6.mp4")
        val process = MediaProcess()
        process.run(listOf("ffmpeg", "-nostdin", "-v", "error", "-f", "lavfi", "-i", "color=red:s=160x90:r=12:d=1", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-brand", "iso6", fixture.toString()))
        val bytes = Files.readAllBytes(fixture)
        assertThat(String(bytes, 4, 4, Charsets.US_ASCII)).isEqualTo("ftyp")
        assertThat(String(bytes, 8, 4, Charsets.US_ASCII)).isEqualTo("iso6")
        val metadata = mapper.readTree(process.run(listOf("ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=codec_name:format=format_name", "-of", "json", fixture.toString())))
        assertThat(metadata["streams"][0]["codec_name"].asString()).isEqualTo("h264")
        assertThat(metadata["format"]["format_name"].asString().split(',')).contains("mp4")
        mvc.multipart("/api/v1/projects/${project()}/shots") {
            file(MockMultipartFile("file", "iso6.mp4", "video/mp4", bytes))
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("READY") }
            jsonPath("$.kind") { value("VIDEO") }
            jsonPath("$.durationMs") { value(1000) }
            jsonPath("$.frames[0].timestampMs") { value(0) }
        }
    }

    @Test
    fun `ftyp signature alone does not admit corrupt video`() {
        val project = project()
        val bytes = byteArrayOf(0, 0, 0, 12) + "ftypiso6".toByteArray(Charsets.US_ASCII)
        mvc.multipart("/api/v1/projects/$project/shots") {
            file(MockMultipartFile("file", "corrupt.mp4", "video/mp4", bytes))
        }.andExpect { status { isEqualTo(422) } }
        mvc.get("/api/v1/projects/$project/shots").andExpect {
            jsonPath("$[0].status") { value("FAILED") }
            jsonPath("$[0].frames.length()") { value(0) }
        }
        Files.list(directory.resolve("media").resolve(project)).use { assertThat(it.count()).isZero() }
    }

    @Test
    fun `unsupported codec and excessive duration fail inspection and clean files`() {
        val project = project()
        listOf("mpeg4" to "1", "libx264" to "121").forEachIndexed { index, (codec, duration) ->
            val fixture = directory.resolve("invalid-$index.mp4")
            MediaProcess().run(listOf("ffmpeg", "-nostdin", "-v", "error", "-f", "lavfi", "-i", "color=red:s=32x32:r=1:d=$duration", "-c:v", codec, fixture.toString()))
            mvc.multipart("/api/v1/projects/$project/shots") { file(MockMultipartFile("file", "invalid.mp4", "video/mp4", Files.readAllBytes(fixture))) }.andExpect { status { isEqualTo(422) } }
        }
        Files.list(directory.resolve("media").resolve(project)).use { assertThat(it.count()).isZero() }
    }

    @Test
    fun `decoder timeout unavailable executable and output limit are bounded`() {
        val process = MediaProcess()
        assertThatThrownBy { process.run(listOf("sceneproof-nonexistent-ffmpeg")) }.isInstanceOf(MediaFailure::class.java).extracting("code").isEqualTo("DECODER_UNAVAILABLE")
        assertThatThrownBy { process.run(listOf("ffmpeg", "-nostdin", "-v", "error", "-f", "lavfi", "-i", "color=red:s=32x32:r=1", "-f", "null", "-"), 1) }.isInstanceOf(MediaFailure::class.java).extracting("code").isEqualTo("DECODER_TIMEOUT")
        assertThatThrownBy { process.run(listOf("ffmpeg", "-nostdin", "-v", "error", "-f", "lavfi", "-i", "color=red:s=512x512:d=1", "-f", "rawvideo", "-")) }.isInstanceOf(MediaFailure::class.java).extracting("code").isEqualTo("INVALID_VIDEO")
    }

    @Test
    fun `storage rejects traversal keys`() {
        val storage = LocalMediaStorage(directory.resolve("isolated").toString())
        assertThatThrownBy { storage.file(UUID.randomUUID(), UUID.randomUUID(), "../../secret") }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
