package dev.sceneproof.analysis

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.UUID

data class CreateAnalysisRequest(val requestId: UUID)

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
class AnalysisController(private val service: AnalysisService, private val repository: AnalysisRepository) {
    @PostMapping("/analyses")
    fun analyze(@PathVariable projectId: UUID, @Valid @RequestBody request: CreateAnalysisRequest): ResponseEntity<AnalysisRunView> {
        val run = service.analyze(projectId, request.requestId)
        return ResponseEntity.ok().location(URI.create("/api/v1/projects/$projectId/analyses/${run.id}")).body(run)
    }

    @GetMapping("/analyses/{analysisId}")
    fun get(@PathVariable projectId: UUID, @PathVariable analysisId: UUID): AnalysisRunView = repository.get(projectId, analysisId)

    @GetMapping("/findings")
    fun findings(@PathVariable projectId: UUID, @RequestParam(required = false) analysisId: UUID?, @RequestParam(defaultValue = "0") @Min(0) @Max(10000) page: Int): List<FindingView> = repository.findings(projectId, analysisId, page)
}
