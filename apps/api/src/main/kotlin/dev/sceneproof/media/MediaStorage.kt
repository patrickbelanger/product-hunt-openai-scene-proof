package dev.sceneproof.media

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.util.UUID

interface MediaStorage {
    fun directory(projectId: UUID, shotId: UUID): Path
    fun file(projectId: UUID, shotId: UUID, key: String): Path
    fun delete(projectId: UUID, shotId: UUID)
}

@Component
class LocalMediaStorage(@Value("\${sceneproof.media.root:.local/media}") root: String) : MediaStorage {
    private val root = Path.of(root).toAbsolutePath().normalize().also { Files.createDirectories(it) }.toRealPath()

    override fun directory(projectId: UUID, shotId: UUID): Path {
        var directory = root
        listOf(projectId.toString(), shotId.toString()).forEach { segment ->
            directory = directory.resolve(segment)
            if (!Files.exists(directory, NOFOLLOW_LINKS)) Files.createDirectory(directory)
            require(Files.isDirectory(directory, NOFOLLOW_LINKS) && !Files.isSymbolicLink(directory))
            require(directory.toRealPath().startsWith(root))
        }
        return directory
    }

    override fun file(projectId: UUID, shotId: UUID, key: String): Path {
        require(key == "original.bin" || Regex("[0-9]{2}\\.png").matches(key))
        return directory(projectId, shotId).resolve(key).also { require(!Files.isSymbolicLink(it)) }
    }

    override fun delete(projectId: UUID, shotId: UUID) {
        val directory = directory(projectId, shotId)
        Files.list(directory).use { entries -> entries.forEach { Files.delete(it) } }
        Files.delete(directory)
    }
}
