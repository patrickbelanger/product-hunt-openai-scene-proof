package dev.sceneproof.demo

import dev.sceneproof.media.ImageContent
import dev.sceneproof.media.MediaFailure
import dev.sceneproof.project.CreateProjectRequest
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.ObjectMapper
import java.io.File
import java.io.InputStream

data class DemoAsset(val file: String, val sha256: String, val title: String, val guidance: String = "")
data class DemoSourceProvenance(val masterPath: String, val masterFile: String, val masterSha256: String, val masterDurationUs: Long, val sourceStartUs: Long, val sourceEndUs: Long)
data class DemoTemplate(val version: String, val project: CreateProjectRequest, val references: List<DemoAsset>, val shots: List<DemoAsset>, val sourceFilm: DemoAsset? = null, val sourceProvenance: DemoSourceProvenance? = null)

interface DemoTemplateSource {
    fun template(): DemoTemplate
    fun upload(asset: DemoAsset): MultipartFile
}

@Component
class PackagedDemoTemplate(private val mapper: ObjectMapper) : DemoTemplateSource {
    override fun template(): DemoTemplate {
        val resource = ClassPathResource("demo/manifest.json")
        if (!resource.exists()) throw MediaFailure("DEMO_UNAVAILABLE", "The demo film is not installed. Create a project or ask the operator to install the approved demo assets.", 503)
        return resource.inputStream.use { mapper.readValue(it, DemoTemplate::class.java) }.also { template ->
            require(template.version.matches(Regex("[a-z0-9-]{1,80}")))
            require(template.project.name.isNotBlank() && template.project.name.length <= 120)
            require(template.project.description.length <= 2000 && template.project.rules.length <= 8000)
            require(template.references.size in 1..8 && template.shots.size in 1..8)
            (template.references + template.shots).forEach { asset -> upload(asset) }
            template.sourceFilm?.let { upload(it) }
            template.sourceProvenance?.let { provenance ->
                require(template.sourceFilm != null && template.sourceFilm.sha256 != provenance.masterSha256)
                require(provenance.masterPath == "demo/between the lines - demo.mp4" && provenance.masterDurationUs == 92458667L)
                require(provenance.sourceStartUs == 0L && provenance.sourceEndUs == 36291667L)
                upload(DemoAsset(provenance.masterFile, provenance.masterSha256, "Immutable master"))
            }
        }
    }

    override fun upload(asset: DemoAsset): MultipartFile {
        require(asset.file.matches(Regex("[a-z0-9-]+\\.(png|mp4)")))
        val bytes = ClassPathResource("demo/${asset.file}").inputStream.use { it.readNBytes(100 * 1024 * 1024 + 1) }
        require(bytes.size <= 100 * 1024 * 1024 && ImageContent.sha256(bytes) == asset.sha256)
        return DemoUpload(asset.title, bytes)
    }
}

class DemoUpload(private val filename: String, private val content: ByteArray) : MultipartFile {
    override fun getName(): String = "file"
    override fun getOriginalFilename(): String = filename
    override fun getContentType(): String = "application/octet-stream"
    override fun isEmpty(): Boolean = content.isEmpty()
    override fun getSize(): Long = content.size.toLong()
    override fun getBytes(): ByteArray = content.copyOf()
    override fun getInputStream(): InputStream = content.inputStream()
    override fun transferTo(destination: File) { destination.outputStream().use { it.write(content) } }
}
