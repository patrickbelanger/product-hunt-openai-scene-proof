package dev.sceneproof.analysis

import org.springframework.stereotype.Component
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.json.JsonFactory
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Component
class ContinuityResultValidator(private val mapper: ObjectMapper) {
    private val strictJson = JsonMapper.builder(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build()
    val schema: JsonNode = javaClass.getResourceAsStream("/astra/continuity-result.schema.json")!!.use { mapper.readTree(it) }

    fun parse(text: String): ContinuityAnalysisResult {
        try {
            require(text.toByteArray(Charsets.UTF_8).size <= 128 * 1024)
            val document = strictJson.readTree(text)
            validateNode(document, schema)
            return mapper.treeToValue(document, ContinuityAnalysisResult::class.java)
        } catch (_: Exception) {
            throw invalid()
        }
    }

    fun validate(result: ContinuityAnalysisResult, context: AnalysisContext) {
        try {
            validateNode(mapper.valueToTree(result), schema)
            require(result.analysisMetadata.projectId == context.projectId)
            val shots = context.shots.associateBy { it.id }
            val frames = context.shots.flatMap { shot -> shot.frames.map { it.id to shot.id } }.toMap()
            require(result.inspectedShots.map { it.shotId }.toSet() == shots.keys)
            require(result.inspectedShots.size == shots.size)
            result.inspectedShots.forEach { inspected ->
                val selected = shots.getValue(inspected.shotId).frames.map { it.id }.toSet()
                require(inspected.frameIds.size == selected.size && inspected.frameIds.toSet() == selected)
            }
            result.findings.forEach { finding ->
                require(finding.confidence.isFinite())
                require(finding.affectedShotIds.distinct().size == finding.affectedShotIds.size)
                require(finding.relevantFrameIds.distinct().size == finding.relevantFrameIds.size)
                require(finding.affectedShotIds.all { it in shots })
                require(finding.relevantFrameIds.all { frames[it] in finding.affectedShotIds })
                require(finding.affectedShotIds.all { shotId -> finding.relevantFrameIds.any { frames[it] == shotId } })
                require(finding.relevantReferenceIds.isEmpty())
            }
        } catch (_: Exception) {
            throw invalid()
        }
    }

    private fun validateNode(node: JsonNode, definition: JsonNode) {
        when (definition["type"].asString()) {
            "object" -> {
                require(node.isObject)
                val properties = definition["properties"]
                val expected = properties.propertyNames().toSet()
                require(node.propertyNames().toSet() == expected)
                expected.forEach { name -> validateNode(node[name], properties[name]) }
            }
            "array" -> {
                require(node.isArray)
                require(node.size() in definition["minItems"].asInt()..definition["maxItems"].asInt())
                node.forEach { validateNode(it, definition["items"]) }
            }
            "string" -> {
                require(node.isString)
                val value = node.asString()
                require(value.isNotBlank())
                if (definition.has("maxLength")) require(value.codePointCount(0, value.length) <= definition["maxLength"].asInt())
                if (definition.has("enum")) require(definition["enum"].any { it.asString() == value })
                if (definition.has("format")) require(UUID.fromString(value).toString() == value)
            }
            "number" -> {
                require(node.isNumber)
                require(node.asDouble().isFinite() && node.asDouble() in definition["minimum"].asDouble()..definition["maximum"].asDouble())
            }
            else -> error("Unsupported result schema type")
        }
    }

    private fun invalid() = AnalysisFailure("INVALID_MODEL_OUTPUT", "The analysis result was invalid. No findings were saved.", 502)
}
