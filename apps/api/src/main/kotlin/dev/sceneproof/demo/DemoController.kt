package dev.sceneproof.demo

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class OpenDemoRequest(val requestId: UUID)

@RestController
class DemoController(private val service: DemoService) {
    @PostMapping("/api/v1/demo")
    fun open(@RequestBody request: OpenDemoRequest) = service.open(request.requestId)

    @PostMapping("/api/v1/projects/{projectId}/demo/reset")
    fun reset(@PathVariable projectId: UUID) = service.reset(projectId)
}
