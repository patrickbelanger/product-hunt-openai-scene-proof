package dev.sceneproof

import dev.sceneproof.analysis.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.util.UUID

class ContinuityResultValidatorTest {
    private val mapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
    private val validator = ContinuityResultValidator(mapper)
    private val context = AnalysisContext(UUID.randomUUID(), "Original fixture", "", "The square remains red.", listOf(
        AnalysisShot(UUID.randomUUID(), 0, "First", "IMAGE", null, 1, listOf(AnalysisFrame(UUID.randomUUID(), 0, null, 128, 128, byteArrayOf()))),
        AnalysisShot(UUID.randomUUID(), 1, "Second", "IMAGE", null, 1, listOf(AnalysisFrame(UUID.randomUUID(), 0, null, 128, 128, byteArrayOf()))),
    ), emptyList())

    private fun valid() = ContinuityAnalysisResult(
        "The square changes color between shots.",
        listOf(ContinuityFinding(FindingCategory.PROP, FindingSeverity.HIGH, 0.95, "Square color drift", "Color changes.", "Red square", "Blue square", "The explicit continuity rule is contradicted.", context.shots.map { it.id }, context.shots.flatMap { it.frames.map { frame -> frame.id } }, emptyList(), "Keep the square red across both shots.")),
        context.shots.map { InspectedShot(it.id, it.frames.map { frame -> frame.id }) },
        emptyList(), AnalysisMetadata(context.projectId, "1", "SAMPLED_SEQUENCE"),
    )

    @Test
    fun `strict result round trips and validates selected scope`() {
        val parsed = validator.parse(mapper.writeValueAsString(valid()))
        validator.validate(parsed, context)
        assertThat(parsed).isEqualTo(valid())
    }

    @Test
    fun `malformed incomplete extra fields duplicates and trailing JSON fail`() {
        val json = mapper.writeValueAsString(valid())
        listOf("{", "{}", json.replace("\"projectSummary\":", "\"extra\":true,\"projectSummary\":"), json.replace("\"projectSummary\":", "\"projectSummary\":\"duplicate\",\"projectSummary\":"), "$json {}", json.replace("\"confidence\":0.95", "\"confidence\":\"0.95\"")).forEach {
            assertThatThrownBy { validator.parse(it) }.isInstanceOf(AnalysisFailure::class.java)
        }
    }

    @Test
    fun `invalid category confidence blank strings and excessive output fail`() {
        val json = mapper.writeValueAsString(valid())
        listOf(json.replace("\"PROP\"", "\"INVENTED\""), json.replace("0.95", "1.2"), json.replace("Square color drift", " "), json.replace("Square color drift", "x".repeat(161)), " ".repeat(128 * 1024 + 1)).forEach {
            assertThatThrownBy { validator.parse(it) }.isInstanceOf(AnalysisFailure::class.java)
        }
    }

    @Test
    fun `foreign project and invented shot or frame identifiers fail`() {
        val result = valid()
        val finding = result.findings.single()
        val invalid = listOf(
            result.copy(analysisMetadata = result.analysisMetadata.copy(projectId = UUID.randomUUID())),
            result.copy(findings = listOf(finding.copy(affectedShotIds = listOf(UUID.randomUUID())))),
            result.copy(findings = listOf(finding.copy(relevantFrameIds = listOf(UUID.randomUUID())))),
            result.copy(findings = listOf(finding.copy(relevantReferenceIds = listOf(UUID.randomUUID())))),
        )
        invalid.forEach { assertThatThrownBy { validator.validate(it, context) }.isInstanceOf(AnalysisFailure::class.java) }
    }

    @Test
    fun `evidence must belong to affected shots and cover every affected shot`() {
        val result = valid()
        val finding = result.findings.single()
        listOf(
            finding.copy(affectedShotIds = listOf(context.shots.first().id)),
            finding.copy(relevantFrameIds = listOf(context.shots.first().frames.single().id)),
            finding.copy(affectedShotIds = finding.affectedShotIds + finding.affectedShotIds.first()),
            finding.copy(relevantFrameIds = finding.relevantFrameIds + finding.relevantFrameIds.first()),
        ).forEach { assertThatThrownBy { validator.validate(result.copy(findings = listOf(it)), context) }.isInstanceOf(AnalysisFailure::class.java) }
    }

    @Test
    fun `inspected manifest must exactly cover submitted shots and frames`() {
        val result = valid()
        listOf(
            result.inspectedShots.take(1),
            listOf(result.inspectedShots.first(), result.inspectedShots.first()),
            listOf(result.inspectedShots.first().copy(frameIds = listOf(UUID.randomUUID())), result.inspectedShots.last()),
        ).forEach { assertThatThrownBy { validator.validate(result.copy(inspectedShots = it), context) }.isInstanceOf(AnalysisFailure::class.java) }
    }

    @Test
    fun `no findings is valid only with a complete inspected manifest`() {
        validator.validate(valid().copy(findings = emptyList()), context)
    }

    @Test
    fun `submitted references accept zero through eight citations but reject duplicates and foreign contexts`() {
        val references = List(8) { AnalysisReference(UUID.randomUUID(), context.projectId, "Declared prop", "Keep it red", 128, 128, "a".repeat(64), byteArrayOf()) }
        val supplied = context.copy(references = references)
        val result = valid()
        for (count in 0..8) {
            val cited = result.copy(findings = listOf(result.findings.single().copy(relevantReferenceIds = references.take(count).map { it.id })))
            validator.validate(validator.parse(mapper.writeValueAsString(cited)), supplied)
        }
        val duplicate = result.copy(findings = listOf(result.findings.single().copy(relevantReferenceIds = listOf(references.first().id, references.first().id))))
        assertThatThrownBy { validator.validate(duplicate, supplied) }.isInstanceOf(AnalysisFailure::class.java)
        val tooMany = result.copy(findings = listOf(result.findings.single().copy(relevantReferenceIds = references.map { it.id } + UUID.randomUUID())))
        assertThatThrownBy { validator.parse(mapper.writeValueAsString(tooMany)) }.isInstanceOf(AnalysisFailure::class.java)
        assertThatThrownBy { validator.validate(result, supplied.copy(references = listOf(references.first().copy(projectId = UUID.randomUUID())))) }.isInstanceOf(AnalysisFailure::class.java)
    }

    @Test
    fun `frame selection preserves first middle last and hard bounds`() {
        assertThat(AnalysisContextAssembler.selectedPositions(1)).containsExactly(0)
        assertThat(AnalysisContextAssembler.selectedPositions(2)).containsExactly(0, 1)
        assertThat(AnalysisContextAssembler.selectedPositions(32)).containsExactly(0, 15, 31)
        for (count in 1..32) {
            val selected = AnalysisContextAssembler.selectedPositions(count)
            assertThat(selected).hasSizeLessThanOrEqualTo(3).isSorted()
            assertThat(selected.first()).isZero()
            assertThat(selected.last()).isEqualTo(count - 1)
        }
    }
}
