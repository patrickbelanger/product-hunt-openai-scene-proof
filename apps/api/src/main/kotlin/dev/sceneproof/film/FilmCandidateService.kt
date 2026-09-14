package dev.sceneproof.film

import dev.sceneproof.analysis.AnalysisFailure
import dev.sceneproof.demo.DemoUpload
import dev.sceneproof.media.ImageContent
import dev.sceneproof.media.MediaRepository
import dev.sceneproof.media.MediaStorage
import dev.sceneproof.reference.ReferenceService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FilmCandidateService(private val repository: FilmRepository, private val media: MediaRepository, private val storage: MediaStorage, private val references: ReferenceService) {
    @Transactional
    fun decide(projectId: UUID, id: UUID, request: DecideCandidateRequest): FilmCandidate {
        val candidate = repository.candidate(projectId, id, true)
        if (request.status == CandidateStatus.PENDING) throw AnalysisFailure("INVALID_CANDIDATE_DECISION", "Choose Accept, Edit or Reject.", 400)
        val title = if (request.status == CandidateStatus.ACCEPTED) candidate.proposal.title else request.title.trim()
        val rule = if (request.status == CandidateStatus.ACCEPTED) candidate.proposal.rule else request.rule.trim()
        val scope = if (request.status == CandidateStatus.ACCEPTED) candidate.proposal.scope else request.scope.trim()
        if (request.status != CandidateStatus.REJECTED && (title.isBlank() || rule.isBlank() || scope.isBlank())) throw AnalysisFailure("INVALID_CANDIDATE_DECISION", "Confirmed anchors need a title, rule and narrative scope.", 400)
        if (candidate.status != CandidateStatus.PENDING) {
            if (candidate.status != request.status || (request.status != CandidateStatus.REJECTED && (candidate.confirmedTitle != title || candidate.confirmedRule != rule || candidate.confirmedScope != scope))) throw AnalysisFailure("CANDIDATE_ALREADY_REVIEWED", "This candidate already has a different creator decision.", 409)
            return candidate
        }
        repository.decide(id, request.status, title.takeIf { request.status != CandidateStatus.REJECTED }, rule.takeIf { request.status != CandidateStatus.REJECTED }, scope.takeIf { request.status != CandidateStatus.REJECTED })
        return repository.candidate(projectId, id)
    }

    @Transactional
    fun promote(projectId: UUID, id: UUID): FilmCandidate {
        val candidate = repository.candidate(projectId, id, true)
        if (candidate.status !in setOf(CandidateStatus.ACCEPTED, CandidateStatus.EDITED)) throw AnalysisFailure("CANDIDATE_NOT_CONFIRMED", "Confirm this candidate before promoting a visual reference.", 409)
        if (candidate.referenceId != null) return candidate
        val frameId = candidate.proposal.evidence.frameIds.firstOrNull() ?: throw AnalysisFailure("NO_VISUAL_EVIDENCE", "This anchor has no visual evidence to promote.", 409)
        val (owner, key) = media.content(projectId, frameId)
        val frame = media.list(projectId).flatMap { it.frames }.single { it.id == frameId }
        val bytes = ImageContent.readNormalized(storage.file(projectId, owner, key), frame.width, frame.height)
        val guidance = "Creator-confirmed anchor: ${candidate.confirmedTitle}. Applies only within: ${candidate.confirmedScope}. Use the confirmed film-memory rule; this image alone does not establish a global invariant."
        val reference = references.create(projectId, DemoUpload("Confirmed film anchor.png", bytes), requireNotNull(candidate.confirmedTitle), guidance)
        repository.linkReference(id, reference.id)
        return repository.candidate(projectId, id)
    }
}
