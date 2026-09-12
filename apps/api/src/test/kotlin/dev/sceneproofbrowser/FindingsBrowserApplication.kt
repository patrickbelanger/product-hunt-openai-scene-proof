package dev.sceneproofbrowser

import dev.sceneproof.SceneProofApplication
import dev.sceneproof.analysis.*
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@TestConfiguration(proxyBeanMethods = false)
class FindingsBrowserConfiguration {
    @Bean
    @Primary
    fun browserAnalysisPort(): ContinuityAnalysisPort = object : ContinuityAnalysisPort {
        override fun analyze(context: AnalysisContext): AnalysisCompletion = AnalysisCompletion(
            ContinuityAnalysisResult(
                "Deterministic browser test result",
                listOf(ContinuityFinding(
                    FindingCategory.PROP, FindingSeverity.HIGH, 0.9,
                    "The square changes color", "The red square becomes blue across the sequence.",
                    "The square stays red.", "The square is blue in the later shot.",
                    "The project rule requires a red square throughout this sequence.",
                    context.shots.map { it.id }, context.shots.flatMap { shot -> shot.frames.map { it.id } },
                    emptyList(), "Keep the square red in every shot; preserve its shape and position.",
                )),
                context.shots.map { shot -> InspectedShot(shot.id, shot.frames.map { it.id }) },
                emptyList(), AnalysisMetadata(context.projectId, "1", "SAMPLED_SEQUENCE"),
            ), ANALYSIS_MODEL, null, null, null,
        )
    }

    @RestController
    class BrowserTestMarker {
        @GetMapping("/__test/provider")
        fun marker() = mapOf("provider" to "deterministic-test-only")
    }
}

fun main(args: Array<String>) {
    SpringApplicationBuilder(SceneProofApplication::class.java, FindingsBrowserConfiguration::class.java)
        .profiles("browser")
        .run(*args)
}
