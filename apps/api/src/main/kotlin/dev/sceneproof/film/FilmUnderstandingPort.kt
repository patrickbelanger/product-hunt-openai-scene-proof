package dev.sceneproof.film

import dev.sceneproof.analysis.AnalysisFrame
import dev.sceneproof.analysis.AnalysisUsage
import dev.sceneproof.media.ShotView
import java.time.Instant
import java.util.UUID

enum class FilmStage { PREPARING_SOURCE, DETECTING_STRUCTURE, TRANSCRIBING_AUDIO, UNDERSTANDING_FILM, BUILDING_CANDIDATES, SUCCEEDED, FAILED }
enum class CandidateStatus { PENDING, ACCEPTED, EDITED, REJECTED }
data class SourceFilm(val id: UUID, val projectId: UUID, val name: String, val sha256: String, val byteSize: Long, val durationMs: Long, val createdAt: Instant)
data class FilmSegment(val id: UUID, val position: Int, val startMs: Long, val endMs: Long, val shot: ShotView)
data class TranscriptSegment(val id: UUID, val startMs: Long, val endMs: Long, val text: String, val timestampOrigin: String = "WHISPER_SEGMENT_ESTIMATE_SOURCE_START")
data class FilmStageView(val stage: FilmStage, val startedAt: Instant, val completedAt: Instant?)
data class FilmRun(
    val id: UUID, val projectId: UUID, val sourceFilmId: UUID, val requestId: UUID,
    val stage: FilmStage, val startedAt: Instant, val completedAt: Instant?,
    val failureCode: String?, val failureMessage: String?, val stages: List<FilmStageView>,
    val model: String, val reasoning: String, val transcriptionModel: String, val audioStatus: String?,
    val audioDurationMs: Long?, val transcriptionRequestId: String?, val providerResponseId: String?,
    val providerRequestId: String?, val usage: AnalysisUsage?, val result: FilmUnderstandingResult?,
)
data class FilmEvidence(val frameIds: List<UUID>, val transcriptSegmentIds: List<UUID>)
data class FilmEntity(val localId: String, val name: String, val description: String, val evidence: FilmEvidence)
data class CandidateAnchor(val title: String, val rule: String, val scope: String, val entityIds: List<String>, val evidence: FilmEvidence, val uncertainty: String)
data class NarrativeCue(val title: String, val interpretation: String, val evidence: FilmEvidence, val uncertainty: String)
data class FilmConcern(val title: String, val explanation: String, val evidence: FilmEvidence, val uncertainty: String)
data class FilmUnderstandingResult(
    val projectId: UUID, val sourceFilmId: UUID, val schemaVersion: String,
    val summary: String, val inspectedSegmentIds: List<UUID>, val inspectedFrameIds: List<UUID>,
    val entities: List<FilmEntity>, val candidates: List<CandidateAnchor>,
    val narrativeCues: List<NarrativeCue>, val potentialConcerns: List<FilmConcern>, val warnings: List<String>,
)
data class FilmCandidate(val id: UUID, val runId: UUID, val proposal: CandidateAnchor, val status: CandidateStatus,
    val confirmedTitle: String?, val confirmedRule: String?, val confirmedScope: String?, val decidedAt: Instant?, val referenceId: UUID?)
data class FilmUnderstandingContext(val source: SourceFilm, val segments: List<FilmSegment>, val frames: List<AnalysisFrame>, val transcript: List<TranscriptSegment>, val audioStatus: String)
data class FilmUnderstandingCompletion(val result: FilmUnderstandingResult, val providerResponseId: String?, val providerRequestId: String?, val usage: AnalysisUsage?)
interface FilmUnderstandingPort { fun understand(context: FilmUnderstandingContext): FilmUnderstandingCompletion }

data class AudioInput(val wav: ByteArray, val durationMs: Long)
data class TranscribedSegment(val startMs: Long, val endMs: Long, val text: String)
data class TranscriptionCompletion(val segments: List<TranscribedSegment>, val providerRequestId: String?)
interface AudioTranscriptionPort { fun transcribe(input: AudioInput): TranscriptionCompletion }

data class ConfirmedFilmAnchor(val candidateId: UUID, val sourceFilmId: UUID, val runId: UUID, val title: String, val rule: String, val scope: String)
data class FilmContextEvidence(val id: UUID, val sourceFilmId: UUID, val runId: UUID, val kind: String, val text: String, val startMs: Long?, val endMs: Long?, val timestampOrigin: String, val frameIds: List<UUID> = emptyList(), val transcriptSegmentIds: List<UUID> = emptyList())
data class ContinuityFilmMemory(val anchors: List<ConfirmedFilmAnchor> = emptyList(), val evidence: List<FilmContextEvidence> = emptyList())
