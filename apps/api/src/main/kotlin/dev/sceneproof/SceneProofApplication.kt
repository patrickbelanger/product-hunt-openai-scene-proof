package dev.sceneproof

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SceneProofApplication

fun main(args: Array<String>) {
    runApplication<SceneProofApplication>(*args)
}
