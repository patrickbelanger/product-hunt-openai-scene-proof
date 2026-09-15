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

    @Test fun `storage pressure prevents new files but does not prevent cleanup`() {
        val root = directory.resolve("media")
        val project = UUID.randomUUID()
        val asset = UUID.randomUUID()
        val storage = LocalMediaStorage(root.toString())
        Files.write(storage.file(project, asset, "original.bin"), byteArrayOf(1))
        val pressured = LocalMediaStorage(root.toString(), Long.MAX_VALUE)
        assertThatThrownBy { pressured.directory(project, UUID.randomUUID()) }.isInstanceOf(dev.sceneproof.media.MediaFailure::class.java)
        pressured.delete(project, asset)
        assertThat(Files.exists(root.resolve(project.toString()).resolve(asset.toString()))).isFalse()
    }

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

    @Test fun `linked project cannot read or delete external or another project media`() {
        val root = directory.resolve("media")
        val storage = LocalMediaStorage(root.toString())
        val target = Files.createDirectory(directory.resolve("packaged-demo"))
        val sentinel = Files.write(target.resolve("master.mp4"), byteArrayOf(7))
        val project = UUID.randomUUID()
        val link = root.resolve(project.toString())
        if (System.getProperty("os.name").startsWith("Windows")) {
            val command = "New-Item -ItemType Junction -Path '${link.toString().replace("'", "''")}' -Target '${target.toString().replace("'", "''")}' | Out-Null"
            val process = ProcessBuilder("powershell.exe", "-NoProfile", "-Command", command).redirectErrorStream(true).start()
            assertThat(process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue()
            assertThat(process.exitValue()).isZero()
        } else Files.createSymbolicLink(link, target)
        try {
            assertThatThrownBy { storage.directory(project, UUID.randomUUID()) }.isInstanceOf(IllegalArgumentException::class.java)
            assertThatThrownBy { storage.deleteProject(project) }.isInstanceOf(IllegalArgumentException::class.java)
            assertThat(Files.readAllBytes(sentinel)).containsExactly(7)
        } finally { Files.delete(link) }
    }

    @Test fun `asset cleanup never creates a missing directory and works after project tombstone`() {
        val root = directory.resolve("media")
        val storage = LocalMediaStorage(root.toString())
        val project = UUID.randomUUID()
        storage.delete(project, UUID.randomUUID())
        assertThat(Files.exists(root.resolve(project.toString()))).isFalse()
        storage.deleteProject(project)
        storage.delete(project, UUID.randomUUID())
        assertThat(Files.exists(root.resolve(project.toString()))).isFalse()
    }

    @Test fun `rejects traversal absolute paths alternate streams and nonregular file keys`() {
        val storage = LocalMediaStorage(directory.resolve("media").toString())
        val project = UUID.randomUUID()
        val asset = UUID.randomUUID()
        listOf("../original.bin", "..\\original.bin", "/original.bin", "C:\\original.bin", "00.png:stream", "00.png/../original.bin").forEach { key ->
            assertThatThrownBy { storage.file(project, asset, key) }.isInstanceOf(IllegalArgumentException::class.java)
        }
        Files.createDirectory(storage.directory(project, asset).resolve("00.png"))
        assertThatThrownBy { storage.file(project, asset, "00.png") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { storage.delete(project, asset) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
