package dev.sceneproof.analysis

import org.springframework.stereotype.Component
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.json.JsonFactory
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

@Component
class TargetedResultValidator(private val mapper: ObjectMapper, private val continuity: ContinuityResultValidator) {
    val schema = javaClass.getResourceAsStream("/astra/targeted-result.schema.json")!!.use { mapper.readTree(it) }
    private val strictJson = JsonMapper.builder(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build()

    fun parse(text: String): TargetedResult = checked {
        require(text.toByteArray(Charsets.UTF_8).size <= 128 * 1024)
        val document = strictJson.readTree(text)
        continuity.validateNode(document, schema)
        mapper.treeToValue(document, TargetedResult::class.java)
    }

    fun validate(result: TargetedResult, context: TargetedContext) = checked {
        continuity.validateNode(mapper.valueToTree(result), schema)
        require(result.projectId == context.sequence.projectId && result.originalFindingId == context.originalFinding.id)
        require(result.affectedShotIds.size == context.affectedShotIds.size && result.affectedShotIds.toSet() == context.affectedShotIds.toSet())
        val shots = context.sequence.shots.associateBy { it.id }
        val owners = context.sequence.shots.flatMap { shot -> shot.frames.map { it.id to shot.id } }.toMap()
        require(result.inspectedShots.size == shots.size && result.inspectedShots.map { it.shotId }.toSet() == shots.keys)
        result.inspectedShots.forEach { inspected ->
            val expected = shots.getValue(inspected.shotId).frames.map { it.id }.toSet()
            require(inspected.frameIds.size == expected.size && inspected.frameIds.toSet() == expected)
        }
        require(result.evidenceFrameIds.distinct().size == result.evidenceFrameIds.size)
        require(result.evidenceFrameIds.all { it in owners })
        require(result.evidenceFrameIds.containsAll(context.originalFinding.relevantFrameIds))
        require(result.affectedShotIds.all { shotId -> result.evidenceFrameIds.any { owners[it] == shotId } })
        if (result.outcome == IntentOutcome.ISSUE_REMAINS) require(result.remainingIssue.isNotBlank() && result.suggestedCorrection.isNotBlank())
        if (result.outcome == IntentOutcome.INTENT_ACCEPTED) require(result.remainingIssue.isEmpty() && result.suggestedCorrection.isEmpty())
    }

    private fun <Result> checked(block: () -> Result): Result = try { block() } catch (_: Exception) {
        throw AnalysisFailure("INVALID_MODEL_OUTPUT", "The targeted judgement was invalid. The previous finding remains unchanged.", 502)
    }
}
