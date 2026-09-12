package dev.sceneproof.analysis

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.Semaphore

@Service
class AnalysisService(private val repository: AnalysisRepository, private val assembler: AnalysisContextAssembler, private val port: ContinuityAnalysisPort, private val validator: ContinuityResultValidator) {
    private val permits = Semaphore(1)
    private val log = LoggerFactory.getLogger(AnalysisService::class.java)

    fun analyze(projectId: UUID, requestId: UUID): AnalysisRunView {
        if (!permits.tryAcquire()) {
            repository.findRequest(projectId, requestId)?.let { return it }
            throw AnalysisFailure("ANALYSIS_BUSY", "Another analysis is running. Retry later with the same requestId.", 429)
        }
        try {
            val (run, created) = repository.start(projectId, requestId)
            if (!created) return run
            var completion: AnalysisCompletion? = null
            try {
                val context = assembler.assemble(projectId)
                repository.recordContext(run.id, context)
                completion = port.analyze(context)
                validator.validate(completion.result, context)
                require(completion.model == ANALYSIS_MODEL)
                repository.succeed(run.id, context, completion)
            } catch (exception: Exception) {
                val classified = exception as? AnalysisFailure
                val failure = AnalysisFailure(classified?.code ?: if (exception is org.springframework.dao.DataAccessException) "ANALYSIS_PERSISTENCE_FAILURE" else "ANALYSIS_INTERNAL_FAILURE",
                    classified?.detail ?: "The analysis could not be completed or stored. No automatic retry was made.", classified?.httpStatus ?: 503,
                    classified?.usage ?: completion?.usage, classified?.providerResponseId ?: completion?.providerResponseId, classified?.providerRequestId ?: completion?.providerRequestId)
                if (classified == null) log.error("Unexpected analysis failure for run {}", run.id, safeTrace(exception))
                failure.analysisRunId = run.id
                try { repository.fail(run.id, failure) } catch (persistence: Exception) {
                    log.error("Could not persist failure for analysis {}", run.id, safeTrace(persistence))
                }
                log.warn("Analysis {} failed with {}", run.id, failure.code)
                throw failure
            }
            return repository.get(projectId, run.id)
        } finally { permits.release() }
    }

    private fun safeTrace(exception: Throwable): Throwable = RuntimeException(exception.javaClass.name).apply {
        stackTrace = exception.stackTrace
    }
}
