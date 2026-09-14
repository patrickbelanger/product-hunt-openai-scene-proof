package dev.sceneproof.analysis

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FindingActionService(private val actions: FindingActionRepository, private val analyses: AnalysisRepository, private val assembler: AnalysisContextAssembler, private val port: ContinuityAnalysisPort, private val validator: TargetedResultValidator) {
    private val log = LoggerFactory.getLogger(FindingActionService::class.java)

    fun act(projectId: UUID, findingId: UUID, request: CreateFindingActionRequest): FindingActionView {
        val (action, created) = try { actions.start(projectId, findingId, request) } catch (_: org.springframework.dao.DataAccessException) {
            throw AnalysisFailure("ANALYSIS_PERSISTENCE_FAILURE", "The creator action could not be reserved. Recover with the same requestId.", 503)
        }
        if (!created || action.reanalysis == null) return action
        val runId = action.reanalysis.id
        var completion: TargetedCompletion? = null
        try {
            val original = analyses.finding(projectId, findingId)
            val sequence = assembler.assemble(projectId, original).copy(filmMemory = analyses.originalFilmMemory(projectId, original.analysisRunId))
            require(sequence.shots.flatMap { it.frames.map { frame -> frame.id } }.containsAll(original.relevantFrameIds))
            val previous = action.supersedesActionId?.let { actions.get(projectId, findingId, it).result }
            val originalRules = analyses.originalRules(projectId, original.analysisRunId)
            val context = TargetedContext(sequence, original.copy(status = "OPEN"), action.explanation, action.scope, action.affectedShotIds, previous, originalRules)
            analyses.recordContext(runId, sequence, "original-evidence-neighbors-v1", originalRules)
            completion = port.reanalyze(context)
            validator.validate(completion.result, context)
            require(completion.model == ANALYSIS_MODEL)
            actions.succeed(action, sequence, completion)
        } catch (exception: Exception) {
            val classified = exception as? AnalysisFailure
            val failure = AnalysisFailure(classified?.code ?: if (exception is org.springframework.dao.DataAccessException) "ANALYSIS_PERSISTENCE_FAILURE" else "ANALYSIS_INTERNAL_FAILURE",
                classified?.detail ?: "The re-evaluation could not be completed or stored. The previous judgement remains unchanged.", classified?.httpStatus ?: 503,
                classified?.usage ?: completion?.usage, classified?.providerResponseId ?: completion?.providerResponseId, classified?.providerRequestId ?: completion?.providerRequestId)
            try { analyses.fail(runId, failure) } catch (_: Exception) { log.error("Could not persist targeted failure for run {}", runId) }
            log.warn("Targeted analysis {} failed with {}", runId, failure.code)
            failure.analysisRunId = runId
            throw failure
        }
        return actions.get(projectId, findingId, action.id)
    }
}
