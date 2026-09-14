package dev.sceneproof

import dev.sceneproof.film.*

object FilmFixtures {
    fun completion(context: FilmUnderstandingContext): FilmUnderstandingCompletion {
        val evidence = FilmEvidence(context.frames.take(2).map { it.id }, context.transcript.take(1).map { it.id })
        return FilmUnderstandingCompletion(FilmUnderstandingResult(context.source.projectId, context.source.id, "1", "Deterministic test film summary",
            context.segments.map { it.id }, context.frames.map { it.id },
            listOf(FilmEntity("shape", "Test shape", "A recurring synthetic shape", evidence)),
            listOf(CandidateAnchor("Shape color", "Keep the shape red", "Within the test scene", listOf("shape"), evidence, "Lighting may explain differences"),
                CandidateAnchor("Shape position", "Keep the shape centered", "Within the test scene", listOf("shape"), evidence, ""),
                CandidateAnchor("Shape scale", "Keep the scale consistent", "Within the test scene", listOf("shape"), evidence, "")),
            listOf(NarrativeCue("Test narration", "Narration is contextual and may describe the image", evidence, "No literal event is established")),
            listOf(FilmConcern("Check the shape", "A test-only concern to review against context", evidence, "Not an established error")),
            listOf("Deterministic test-only result; sampled coverage")), "resp_film_fixture", "req_film_fixture", null)
    }
}
