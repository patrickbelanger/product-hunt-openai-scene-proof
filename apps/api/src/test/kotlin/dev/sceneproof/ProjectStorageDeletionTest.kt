package dev.sceneproof

import dev.sceneproof.media.LocalMediaStorage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class ProjectStorageDeletionTest {
    @TempDir lateinit var directory: Path

    @Test fun `deletes only the UUID owned tree including generated artifacts and blocks stale recreation`() {
        val storage = LocalMediaStorage(directory.resolve("media").toString())
        val project = UUID.randomUUID()
        val artifact = storage.directory(project, UUID.randomUUID()).resolve("audio.wav")
        Files.write(artifact, byteArrayOf(1))
        val other = storage.file(UUID.randomUUID(), UUID.randomUUID(), "original.bin")
        Files.write(other, byteArrayOf(2))
        val packaged = directory.resolve("master.mp4")
        Files.write(packaged, byteArrayOf(3))
        storage.deleteProject(project)
        storage.deleteProject(project)
        assertThat(Files.exists(artifact)).isFalse()
        assertThat(Files.readAllBytes(other)).containsExactly(2)
        assertThat(Files.readAllBytes(packaged)).containsExactly(3)
        assertThatThrownBy { storage.directory(project, UUID.randomUUID()) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test fun `missing project directory cleanup is idempotent without touching root`() {
        val root = directory.resolve("media")
        val storage = LocalMediaStorage(root.toString())
        storage.deleteProject(UUID.randomUUID())
        assertThat(Files.isDirectory(root)).isTrue()
    }
}
