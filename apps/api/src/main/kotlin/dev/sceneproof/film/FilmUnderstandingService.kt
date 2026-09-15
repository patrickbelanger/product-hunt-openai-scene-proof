package dev.sceneproof.film

import dev.sceneproof.analysis.AnalysisFailure
import dev.sceneproof.media.ImageContent
import dev.sceneproof.media.MediaFailure
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FilmUnderstandingService(
    private val repository: FilmRepository, private val sources: SourceFilmService,
    private val transcription: AudioTranscriptionPort, private val understanding: FilmUnderstandingPort,
    private val validator: FilmResultValidator,
    private val work: dev.sceneproof.analysis.PaidWorkGate,
) {
    private val log = LoggerFactory.getLogger(FilmUnderstandingService::class.java)

    fun start(projectId: UUID, sourceId: UUID, requestId: UUID): FilmRun {
        val (run, created) = repository.start(projectId, sourceId, requestId)
        if (created) {
            try { Thread.ofVirtual().name("film-understanding-${run.id}").start { execute(run) } }
            catch (_: Exception) { repository.fail(run.id, AnalysisFailure("FILM_WORKER_UNAVAILABLE", "The Film Understanding worker could not start.", 503)) }
        }
        return run
    }

    internal fun execute(run: FilmRun) {
        var completion: FilmUnderstandingCompletion? = null
        var acquired = false
        try {
            work.acquire()
            acquired = true
            val source = requireNotNull(repository.source(run.projectId))
            require(source.id == run.sourceFilmId)
            sources.verify(source)
            repository.advance(run.id, FilmStage.PREPARING_SOURCE, FilmStage.DETECTING_STRUCTURE)
            val segments = sources.structure(source, run.id)
            val frames = sources.frames(source, segments)
            repository.advance(run.id, FilmStage.DETECTING_STRUCTURE, FilmStage.TRANSCRIBING_AUDIO)
            val audio = sources.audio(source, run.id)
            val transcript = audio?.let { transcription.transcribe(it) }
            val audioStatus = if (audio == null) "NO_AUDIO_STREAM" else if (transcript?.segments.isNullOrEmpty()) "NO_TRANSCRIBED_SPEECH" else "TRANSCRIBED"
            repository.audio(run, audioStatus, audio?.let { ImageContent.sha256(it.wav) }, audio?.durationMs, transcript)
            repository.advance(run.id, FilmStage.TRANSCRIBING_AUDIO, FilmStage.UNDERSTANDING_FILM)
            val context = FilmUnderstandingContext(source, segments, frames, repository.transcript(run.projectId, run.id), audioStatus)
            completion = understanding.understand(context)
            repository.advance(run.id, FilmStage.UNDERSTANDING_FILM, FilmStage.BUILDING_CANDIDATES)
            validator.validate(completion.result, context)
            repository.succeed(run, completion)
        } catch (exception: Exception) {
            val known = exception as? AnalysisFailure
            val media = exception as? MediaFailure
            val failure = AnalysisFailure(known?.code ?: media?.code ?: "FILM_PROCESSING_FAILED", known?.detail ?: media?.detail ?: "Film Understanding could not complete or persist. No automatic retry was made.", known?.httpStatus ?: 503,
                known?.usage ?: completion?.usage, known?.providerResponseId ?: completion?.providerResponseId, known?.providerRequestId ?: completion?.providerRequestId)
            try { repository.fail(run.id, failure) } catch (_: Exception) { log.warn("Film failure persistence unavailable for {}", run.id) }
            log.warn("Film Understanding {} failed with {}", run.id, failure.code)
        } finally { if (acquired) work.release() }
    }
}
