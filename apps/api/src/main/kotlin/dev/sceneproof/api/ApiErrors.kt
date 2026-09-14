package dev.sceneproof.api

import dev.sceneproof.project.ProjectNotFound
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI

@RestControllerAdvice
class ApiErrors : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(ApiErrors::class.java)

    @ExceptionHandler(dev.sceneproof.analysis.AnalysisFailure::class)
    fun analysisFailure(failure: dev.sceneproof.analysis.AnalysisFailure): ProblemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.valueOf(failure.httpStatus), failure.detail,
    ).apply {
        type = URI.create("urn:sceneproof:problem:${failure.code.lowercase().replace('_', '-')}")
        failure.analysisRunId?.let { setProperty("analysisRunId", it) }
    }

    @ExceptionHandler(dev.sceneproof.media.MediaFailure::class)
    fun mediaFailure(failure: dev.sceneproof.media.MediaFailure): ProblemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.valueOf(failure.httpStatus), failure.detail,
    ).apply { type = URI.create("urn:sceneproof:problem:${failure.code.lowercase().replace('_', '-')}") }

    @ExceptionHandler(ProjectNotFound::class)
    fun projectNotFound(): ProblemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND, "This project does not exist.",
    ).apply { type = URI.create("urn:sceneproof:problem:project-not-found") }

    @ExceptionHandler(Exception::class)
    fun unexpected(exception: Exception): ResponseEntity<ProblemDetail> {
        log.error(
            "Unexpected request failure: {}",
            exception.javaClass.name,
            RuntimeException(exception.javaClass.name).apply { stackTrace = exception.stackTrace },
        )
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "SceneProof could not complete this request. Please try again.",
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem)
    }
}
