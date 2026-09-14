package dev.sceneproof.film

import dev.sceneproof.analysis.AnalysisFailure
import dev.sceneproof.analysis.ContinuityResultValidator
import org.springframework.stereotype.Component
import tools.jackson.core.StreamReadFeature
import tools.jackson.core.json.JsonFactory
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper

@Component
class FilmResultValidator(private val mapper: ObjectMapper, private val schemas: ContinuityResultValidator) {
    private val strict = JsonMapper.builder(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build()).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build()
    val schema: JsonNode = javaClass.getResourceAsStream("/astra/film-understanding.schema.json")!!.use { mapper.readTree(it) }

    fun parse(text: String): FilmUnderstandingResult = try {
        require(text.toByteArray().size <= 128 * 1024)
        val document = strict.readTree(text)
        schemas.validateNode(document, schema)
        mapper.treeToValue(document, FilmUnderstandingResult::class.java)
    } catch (_: Exception) { throw invalid() }

    fun validate(result: FilmUnderstandingResult, context: FilmUnderstandingContext) {
        try {
            schemas.validateNode(mapper.valueToTree(result), schema)
            require(result.projectId == context.source.projectId && result.sourceFilmId == context.source.id)
            require(result.inspectedSegmentIds.distinct().size == context.segments.size && result.inspectedSegmentIds.toSet() == context.segments.map { it.id }.toSet())
            val frames = context.frames.map { it.id }.toSet()
            require(result.inspectedFrameIds.size == frames.size && result.inspectedFrameIds.toSet() == frames)
            val transcripts = context.transcript.map { it.id }.toSet()
            val entities = result.entities.map { it.localId }.toSet()
            require(entities.size == result.entities.size && entities.all { it.matches(Regex("[a-zA-Z0-9_-]{1,40}")) })
            fun evidence(value: FilmEvidence) {
                require(value.frameIds.isNotEmpty() || value.transcriptSegmentIds.isNotEmpty())
                require(value.frameIds.distinct().size == value.frameIds.size && value.frameIds.all { it in frames })
                require(value.transcriptSegmentIds.distinct().size == value.transcriptSegmentIds.size && value.transcriptSegmentIds.all { it in transcripts })
            }
            result.entities.forEach { evidence(it.evidence); require(it.evidence.frameIds.size >= 2) }
            result.candidates.forEach { evidence(it.evidence); require(it.entityIds.distinct().size == it.entityIds.size && it.entityIds.all { entity -> entity in entities }) }
            result.narrativeCues.forEach { evidence(it.evidence) }
            result.potentialConcerns.forEach { evidence(it.evidence); require(it.evidence.frameIds.isNotEmpty()) }
        } catch (_: Exception) { throw invalid() }
    }

    private fun invalid() = AnalysisFailure("INVALID_FILM_OUTPUT", "Film Understanding returned invalid or ungrounded structured output. No candidates were saved.", 502)
}
