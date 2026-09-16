package dev.sceneproof.media

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.util.UUID

interface MediaStorage {
    fun checkWriteCapacity()
    fun directory(projectId: UUID, shotId: UUID): Path
    fun file(projectId: UUID, shotId: UUID, key: String): Path
    fun delete(projectId: UUID, shotId: UUID)
    fun deleteProject(projectId: UUID)
}

@Component
class LocalMediaStorage(
    @Value("\${sceneproof.media.root:.local/media}") root: String,
    @param:Value("\${sceneproof.media.minimum-free-bytes:536870912}") private val minimumFreeBytes: Long = 536870912,
) : MediaStorage {
    private val root = Path.of(root).toAbsolutePath().normalize().also {
        require(it.parent != null)
        Files.createDirectories(it)
        require(it.toRealPath() == it && !Files.isSymbolicLink(it))
    }

    private fun checkRoot() {
        require(Files.isDirectory(root, NOFOLLOW_LINKS) && !Files.isSymbolicLink(root) && root.toRealPath() == root)
    }

    override fun checkWriteCapacity() {
        checkRoot()
        require(minimumFreeBytes >= 536870912)
        if (Files.getFileStore(root).usableSpace < minimumFreeBytes || Files.getFileStore(Path.of(System.getProperty("java.io.tmpdir"))).usableSpace < minimumFreeBytes) {
            throw MediaFailure("STORAGE_CAPACITY", "Media storage is near its safety reserve. Ask the operator to free space before importing.", 503)
        }
    }

    private fun checkDirectory(directory: Path) {
        require(Files.isDirectory(directory, NOFOLLOW_LINKS) && !Files.isSymbolicLink(directory))
        require(directory.startsWith(root) && directory.toRealPath() == directory)
    }

    override fun directory(projectId: UUID, shotId: UUID): Path {
        checkRoot()
        require(!Files.exists(root.resolve("$projectId.deleted"), NOFOLLOW_LINKS))
        var directory = root
        listOf(projectId.toString(), shotId.toString()).forEach { segment ->
            directory = directory.resolve(segment)
            if (!Files.exists(directory, NOFOLLOW_LINKS)) {
                checkWriteCapacity()
                Files.createDirectory(directory)
            }
            checkDirectory(directory)
        }
        return directory
    }

    override fun file(projectId: UUID, shotId: UUID, key: String): Path {
        require(key == "original.bin" || Regex("[0-9]{2}\\.png").matches(key))
        return directory(projectId, shotId).resolve(key).also {
            require(!Files.isSymbolicLink(it))
            if (Files.exists(it, NOFOLLOW_LINKS)) require(Files.isRegularFile(it, NOFOLLOW_LINKS) && it.toRealPath() == it)
        }
    }

    override fun delete(projectId: UUID, shotId: UUID) {
        checkRoot()
        val project = root.resolve(projectId.toString())
        if (!Files.exists(project, NOFOLLOW_LINKS)) return
        checkDirectory(project)
        val directory = project.resolve(shotId.toString())
        if (!Files.exists(directory, NOFOLLOW_LINKS)) return
        checkDirectory(directory)
        val files = Files.list(directory).use { it.toList() }
        files.forEach { require(Files.isRegularFile(it, NOFOLLOW_LINKS) && !Files.isSymbolicLink(it) && it.toRealPath() == it) }
        files.forEach { Files.delete(it) }
        Files.delete(directory)
    }

    override fun deleteProject(projectId: UUID) {
        checkRoot()
        val directory = root.resolve(projectId.toString()).normalize()
        require(directory.parent == root && directory.startsWith(root))
        val marker = root.resolve("$projectId.deleted")
        if (!Files.exists(marker, NOFOLLOW_LINKS)) Files.createFile(marker)
        require(Files.isRegularFile(marker, NOFOLLOW_LINKS) && !Files.isSymbolicLink(marker))
        if (!Files.exists(directory, NOFOLLOW_LINKS)) return
        checkDirectory(directory)
        val paths = Files.walk(directory).use { it.toList() }
        paths.forEach { path ->
            require(!Files.isSymbolicLink(path) && path.toRealPath() == path && path.startsWith(directory))
            require(Files.isDirectory(path, NOFOLLOW_LINKS) || Files.isRegularFile(path, NOFOLLOW_LINKS))
        }
        paths.sortedByDescending { it.nameCount }.forEach { Files.deleteIfExists(it) }
    }
}
