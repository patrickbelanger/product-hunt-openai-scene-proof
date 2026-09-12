package dev.sceneproof.analysis

import dev.sceneproof.media.MediaRepository
import dev.sceneproof.media.MediaStorage
import dev.sceneproof.project.ProjectService
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.util.UUID
import javax.imageio.ImageIO

@Component
class AnalysisContextAssembler(private val projects: ProjectService, private val media: MediaRepository, private val storage: MediaStorage) {
    fun assemble(projectId: UUID): AnalysisContext {
        val project = projects.get(projectId)
        val imports = media.list(projectId)
        val ready = imports.filter { it.status == "READY" }
        if (ready.isEmpty()) throw AnalysisFailure("NO_USABLE_SHOTS", "Import at least one usable shot before analysis.")
        if (ready.size > 8) throw AnalysisFailure("ANALYSIS_SHOT_LIMIT", "Analysis currently supports at most 8 ready shots. Use a smaller project.")
        var totalBytes = 0L
        val shots = ready.map { shot ->
            if (shot.frames.isEmpty()) throw AnalysisFailure("NO_USABLE_FRAMES", "A ready shot has no representative frames.")
            val selected = selectedPositions(shot.frames.size).map { shot.frames[it] }
            val frames = selected.map { frame ->
                val bytes = try {
                    val (owner, key) = media.content(projectId, frame.id)
                    require(owner == shot.id)
                    val file = storage.file(projectId, owner, key)
                    require(Files.isRegularFile(file) && Files.size(file) in 1..8L * 1024 * 1024)
                    val png = Files.newInputStream(file).use { it.readNBytes(8 * 1024 * 1024 + 1) }
                    require(png.size <= 8 * 1024 * 1024)
                    require(png.take(8) == listOf(137, 80, 78, 71, 13, 10, 26, 10).map { it.toByte() })
                    ImageIO.createImageInputStream(png.inputStream()).use { input ->
                        val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: error("Invalid PNG")
                        try {
                            reader.input = input
                            require(reader.getWidth(0) == frame.width && reader.getHeight(0) == frame.height)
                            require(frame.width in 1..1600 && frame.height in 1..1600)
                            require(reader.read(0) != null)
                        } finally { reader.dispose() }
                    }
                    png
                } catch (_: Exception) {
                    throw AnalysisFailure("FRAME_UNAVAILABLE", "A normalized frame is missing or unusable. Re-import its shot.", 503)
                }
                totalBytes += bytes.size
                if (totalBytes > 16L * 1024 * 1024) throw AnalysisFailure("ANALYSIS_SIZE_LIMIT", "Selected frames exceed the 16 MiB analysis limit.")
                AnalysisFrame(frame.id, frame.position, frame.timestampMs, frame.width, frame.height, bytes)
            }
            AnalysisShot(shot.id, shot.position, shot.name, shot.kind, shot.durationMs, shot.frames.size, frames)
        }
        val warnings = buildList {
            if (imports.size != ready.size) add("Failed import attempts were excluded from analysis.")
            if (shots.any { it.availableFrameCount > it.frames.size }) add("Temporal subsampling may miss brief continuity changes.")
        }
        return AnalysisContext(projectId, project.name, project.description, project.rules, shots, warnings)
    }

    companion object {
        fun selectedPositions(count: Int): List<Int> {
            require(count > 0)
            return listOf(0, (count - 1) / 2, count - 1).distinct()
        }
    }
}
