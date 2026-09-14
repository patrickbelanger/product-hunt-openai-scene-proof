package dev.sceneproof.film

import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Size
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class UnderstandFilmRequest(val requestId: UUID, val sourceFilmId: UUID, @field:AssertTrue val paidConsent: Boolean)
data class DecideCandidateRequest(val status: CandidateStatus, @field:Size(max = 120) val title: String = "", @field:Size(max = 2000) val rule: String = "", @field:Size(max = 1000) val scope: String = "")
data class FilmIntelligence(val source: SourceFilm?, val runs: List<FilmRun>, val segments: List<FilmSegment>, val confirmedAnchors: List<ConfirmedFilmAnchor>)

@RestController
@RequestMapping("/api/v1/projects/{projectId}/film")
class FilmController(private val repository: FilmRepository, private val sources: SourceFilmService, private val service: FilmUnderstandingService, private val candidates: FilmCandidateService) {
    @GetMapping
    fun intelligence(@PathVariable projectId: UUID): ResponseEntity<FilmIntelligence> = privateResponse(FilmIntelligence(repository.source(projectId), repository.list(projectId), repository.segments(projectId), repository.memory(projectId).anchors))

    @PostMapping("/source", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(@PathVariable projectId: UUID, @RequestPart file: MultipartFile): SourceFilm = sources.upload(projectId, file)

    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun start(@PathVariable projectId: UUID, @Valid @RequestBody request: UnderstandFilmRequest): FilmRun = service.start(projectId, request.sourceFilmId, request.requestId)

    @GetMapping("/runs/{runId}")
    fun run(@PathVariable projectId: UUID, @PathVariable runId: UUID): ResponseEntity<FilmRun> = privateResponse(repository.get(projectId, runId))

    @GetMapping("/runs/{runId}/transcript")
    fun transcript(@PathVariable projectId: UUID, @PathVariable runId: UUID): ResponseEntity<List<TranscriptSegment>> = privateResponse(repository.transcript(projectId, runId))

    @GetMapping("/runs/{runId}/candidates")
    fun candidates(@PathVariable projectId: UUID, @PathVariable runId: UUID): ResponseEntity<List<FilmCandidate>> = privateResponse(repository.candidates(projectId, runId))

    @PostMapping("/candidates/{candidateId}/decision")
    fun decide(@PathVariable projectId: UUID, @PathVariable candidateId: UUID, @Valid @RequestBody request: DecideCandidateRequest): FilmCandidate = candidates.decide(projectId, candidateId, request)

    @PostMapping("/candidates/{candidateId}/reference")
    fun promote(@PathVariable projectId: UUID, @PathVariable candidateId: UUID): FilmCandidate = candidates.promote(projectId, candidateId)

    private fun <Body : Any> privateResponse(body: Body): ResponseEntity<Body> = ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body)
}
