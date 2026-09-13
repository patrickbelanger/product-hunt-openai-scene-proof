package dev.sceneproof.analysis

import java.time.Instant
import java.util.UUID

enum class FindingActionType { INTENTIONAL_CHANGE, RESOLVE, DISMISS }
enum class IntentOutcome { INTENT_ACCEPTED, ISSUE_REMAINS, INSUFFICIENT_EVIDENCE }

data class TargetedContext(
    val sequence: AnalysisContext,
    val originalFinding: FindingView,
    val explanation: String,
    val scope: String,
    val affectedShotIds: List<UUID>,
    val previousJudgement: TargetedResult?,
    val originalRules: String? = null,
)

data class TargetedResult(
    val projectId: UUID,
    val originalFindingId: UUID,
    val schemaVersion: String,
    val outcome: IntentOutcome,
    val summary: String,
    val explanation: String,
    val evaluatedScope: String,
    val affectedShotIds: List<UUID>,
    val evidenceFrameIds: List<UUID>,
    val inspectedShots: List<InspectedShot>,
    val remainingIssue: String,
    val suggestedCorrection: String,
)

data class TargetedCompletion(
    val result: TargetedResult,
    val model: String,
    val providerResponseId: String?,
    val providerRequestId: String?,
    val usage: AnalysisUsage?,
)

data class FindingActionView(
    val id: UUID, val projectId: UUID, val findingId: UUID, val originalAnalysisRunId: UUID,
    val requestId: UUID, val type: FindingActionType, val explanation: String, val scope: String,
    val affectedShotIds: List<UUID>, val createdAt: Instant, val supersedesActionId: UUID?,
    val reanalysis: AnalysisRunView?, val result: TargetedResult?,
)
