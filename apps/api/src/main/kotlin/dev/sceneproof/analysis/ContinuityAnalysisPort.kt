package dev.sceneproof.analysis

import java.util.UUID

const val ANALYSIS_MODEL = "gpt-6-astra"

enum class FindingCategory {
    CHARACTER_IDENTITY, CHARACTER_APPEARANCE, HAIR, WARDROBE, PROP, OBJECT_STATE,
    ENVIRONMENT, SPATIAL_CONTINUITY, SCREEN_DIRECTION, LIGHTING, TIME_OF_DAY,
    HAND_OBJECT_INTERACTION, DEVICE_UI, TEXT_CONTINUITY, OTHER,
}

enum class FindingSeverity { LOW, MEDIUM, HIGH }

data class ContinuityFinding(
    val category: FindingCategory,
    val severity: FindingSeverity,
    val confidence: Double,
    val title: String,
    val summary: String,
    val expectedState: String,
    val observedState: String,
    val explanation: String,
    val affectedShotIds: List<UUID>,
    val relevantFrameIds: List<UUID>,
    val relevantReferenceIds: List<UUID>,
    val suggestedCorrectionPrompt: String,
)

data class InspectedShot(val shotId: UUID, val frameIds: List<UUID>)
data class AnalysisMetadata(val projectId: UUID, val schemaVersion: String, val scope: String)
data class ContinuityAnalysisResult(
    val projectSummary: String,
    val findings: List<ContinuityFinding>,
    val inspectedShots: List<InspectedShot>,
    val warnings: List<String>,
    val analysisMetadata: AnalysisMetadata,
)

data class AnalysisUsage(
    val inputTokens: Long?,
    val outputTokens: Long?,
    val totalTokens: Long?,
    val cachedInputTokens: Long?,
    val reasoningTokens: Long?,
    val cacheWriteTokens: Long? = null,
)

data class AnalysisCompletion(
    val result: ContinuityAnalysisResult,
    val model: String,
    val providerResponseId: String?,
    val providerRequestId: String?,
    val usage: AnalysisUsage?,
)

data class AnalysisFrame(val id: UUID, val position: Int, val timestampMs: Long?, val width: Int, val height: Int, val png: ByteArray)
data class AnalysisShot(val id: UUID, val position: Int, val name: String, val kind: String, val durationMs: Long?, val availableFrameCount: Int, val frames: List<AnalysisFrame>)
data class AnalysisContext(val projectId: UUID, val name: String, val description: String, val rules: String, val shots: List<AnalysisShot>, val warnings: List<String>)

interface ContinuityAnalysisPort {
    fun analyze(context: AnalysisContext): AnalysisCompletion
}

class AnalysisFailure(
    val code: String,
    val detail: String,
    val httpStatus: Int = 422,
    val usage: AnalysisUsage? = null,
    val providerResponseId: String? = null,
    val providerRequestId: String? = null,
) : RuntimeException(code) {
    var analysisRunId: UUID? = null
}
