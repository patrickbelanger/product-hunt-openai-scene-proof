package dev.sceneproof.analysis

import dev.sceneproof.media.MediaRepository
import dev.sceneproof.media.MediaStorage
import dev.sceneproof.media.ImageContent
import dev.sceneproof.project.ProjectService
import dev.sceneproof.reference.ReferenceRepository
import dev.sceneproof.reference.ReferenceService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AnalysisContextAssembler(private val projects: ProjectService, private val media: MediaRepository, private val storage: MediaStorage, private val references: ReferenceRepository, private val referenceService: ReferenceService, private val films: dev.sceneproof.film.FilmRepository) {
    fun assemble(projectId: UUID, finding: FindingView? = null): AnalysisContext {
        val project = projects.get(projectId)
        val imports = media.list(projectId)
        val filmSegments = films.segments(projectId).associateBy { it.id }
        val sourceSegments = if (finding == null) filmSegments.keys else emptySet()
        val allReady = imports.filter { it.status == "READY" && (sourceSegments.isEmpty() || it.id in sourceSegments) }
        val ready = if (finding == null) allReady else {
            val affected = allReady.filter { it.id in finding.affectedShotIds }
            require(affected.size == finding.affectedShotIds.size)
            val neighbors = allReady.flatMapIndexed { index, shot ->
                if (shot.id in finding.affectedShotIds) listOfNotNull(allReady.getOrNull(index - 1), allReady.getOrNull(index + 1)) else emptyList()
            }.distinctBy { it.id }.filter { it.id !in finding.affectedShotIds }
            (affected + neighbors.take(8 - affected.size)).sortedBy { it.position }
        }
        if (ready.isEmpty()) throw AnalysisFailure("NO_USABLE_SHOTS", "Import at least one usable shot before analysis.")
        if (ready.size > 8) throw AnalysisFailure("ANALYSIS_SHOT_LIMIT", "Analysis currently supports at most 8 ready shots. Use a smaller project.")
        var totalBytes = 0L
        val shots = ready.map { shot ->
            if (shot.frames.isEmpty()) throw AnalysisFailure("NO_USABLE_FRAMES", "A ready shot has no representative frames.")
            val original = shot.frames.filter { finding?.relevantFrameIds?.contains(it.id) == true }
            val selected = if (original.isNotEmpty()) original else selectedPositions(shot.frames.size).map { shot.frames[it] }
            require(selected.size in 1..3)
            val frames = selected.map { frame ->
                val bytes = try {
                    val (owner, key) = media.content(projectId, frame.id)
                    require(owner == shot.id)
                    ImageContent.readNormalized(storage.file(projectId, owner, key), frame.width, frame.height)
                } catch (_: Exception) {
                    throw AnalysisFailure("FRAME_UNAVAILABLE", "A normalized frame is missing or unusable. Re-import its shot.", 503)
                }
                totalBytes += bytes.size
                if (totalBytes > 16L * 1024 * 1024) throw AnalysisFailure("ANALYSIS_SIZE_LIMIT", "Selected frames exceed the 16 MiB analysis limit.")
                AnalysisFrame(frame.id, frame.position, frame.timestampMs?.let { it + (filmSegments[shot.id]?.startMs ?: 0L) }, frame.width, frame.height, bytes)
            }
            AnalysisShot(shot.id, shot.position, shot.name, shot.kind, shot.durationMs, shot.frames.size, frames)
        }
        val selectedReferences = if (finding == null) references.list(projectId) else references.findingReferences(projectId, finding.id)
        require(selectedReferences.size <= 8)
        if (finding != null) require(selectedReferences.map { it.id }.toSet() == finding.relevantReferenceIds.toSet())
        val referenceContext = selectedReferences.map { reference ->
            val bytes = try { referenceService.content(reference) } catch (_: Exception) {
                throw AnalysisFailure("REFERENCE_UNAVAILABLE", "A required reference image is missing or unusable. Restore its original content before analysis.", 503)
            }
            totalBytes += bytes.size
            if (totalBytes > 16L * 1024 * 1024) throw AnalysisFailure("ANALYSIS_SIZE_LIMIT", "Selected frames and references exceed the shared 16 MiB analysis limit.")
            AnalysisReference(reference.id, projectId, reference.title, reference.guidance, reference.width, reference.height, reference.sha256, bytes)
        }
        val warnings = buildList {
            if (sourceSegments.isNotEmpty()) add("Bounded primary-source segments are reviewed; separately imported clips are outside this sequence. Source frame timestamps share the transcript origin. Sampling may miss brief events.")
            if (imports.any { it.status != "READY" }) add("Failed import attempts were excluded from analysis.")
            if (finding != null) add("Targeted review of original evidence and bounded immediate neighbors; other project shots are outside scope.")
            if (shots.any { it.availableFrameCount > it.frames.size }) add("Temporal subsampling may miss brief continuity changes.")
        }
        return AnalysisContext(projectId, project.name, project.description, project.rules, shots, warnings, referenceContext, films.memory(projectId))
    }

    companion object {
        fun selectedPositions(count: Int): List<Int> {
            require(count > 0)
            return listOf(0, (count - 1) / 2, count - 1).distinct()
        }
    }
}
