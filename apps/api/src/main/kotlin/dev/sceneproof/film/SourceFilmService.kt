package dev.sceneproof.film

import dev.sceneproof.analysis.AnalysisFailure
import dev.sceneproof.analysis.AnalysisFrame
import dev.sceneproof.media.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.util.UUID

@Service
class SourceFilmService(
    private val repository: FilmRepository, private val storage: MediaStorage,
    private val video: FfmpegAdapter, private val gate: MediaIngestionGate,
    private val process: MediaProcess, private val mapper: ObjectMapper,
    @param:Value("\${sceneproof.media.ffmpeg:ffmpeg}") private val ffmpeg: String,
    @param:Value("\${sceneproof.media.ffprobe:ffprobe}") private val ffprobe: String,
) {
    fun upload(projectId: UUID, upload: MultipartFile): SourceFilm {
        repository.source(projectId)
        gate.acquire()
        val id = UUID.randomUUID()
        var committed = false
        try {
            val input = storage.file(projectId, id, "original.bin")
            ImageContent.copy(upload, input, 100L * 1024 * 1024)
            val signature = Files.newInputStream(input).use { it.readNBytes(12) }
            if (signature.size != 12 || String(signature, 4, 4, Charsets.US_ASCII) != "ftyp") throw MediaFailure("UNSUPPORTED_SOURCE", "Use an MP4/H.264 source film.", 415)
            val duration = video.inspect(input)
            val hash = hash(input)
            val name = upload.originalFilename.orEmpty().substringAfterLast('/').substringAfterLast('\\').filter { !it.isISOControl() }.take(160).ifBlank { "Source film" }
            val source = repository.saveSource(projectId, id, name, hash, Files.size(input), duration)
            committed = source.id == id
            return source
        } finally {
            if (!committed) {
                try { if (repository.source(projectId)?.id != id) storage.delete(projectId, id) } catch (_: Exception) { }
            }
            gate.release()
        }
    }

    fun verify(source: SourceFilm) {
        val input = storage.file(source.projectId, source.id, "original.bin")
        if (Files.size(input) != source.byteSize || hash(input) != source.sha256) throw AnalysisFailure("SOURCE_UNAVAILABLE", "The original source film is missing or changed. Restore its original bytes.", 503)
    }

    fun structure(source: SourceFilm, runId: UUID): List<FilmSegment> {
        repository.segments(source.projectId).takeIf { it.isNotEmpty() }?.let { return it }
        gate.acquire()
        try {
            val output = storage.directory(source.projectId, runId)
            val input = storage.file(source.projectId, source.id, "original.bin")
            val origin = videoOrigin(input)
            val media = video.extract(input, output, 768)
            val selected = media.frames.map { it.copy(timestampMs = requireNotNull(it.timestampMs) + origin) }.let { frames ->
                if (frames.size <= 24) frames else (0 until 24).map { index -> frames[index * (frames.size - 1) / 23] }
            }
            require(selected.all { requireNotNull(it.timestampMs) in 0 until source.durationMs })
            val groups = selected.chunked(3)
            val prepared = groups.mapIndexed { position, frames ->
                val start = if (position == 0) 0L else requireNotNull(frames.first().timestampMs)
                val end = groups.getOrNull(position + 1)?.first()?.timestampMs ?: source.durationMs
                val shotId = UUID.randomUUID()
                val copies = frames.mapIndexed { index, frame ->
                    val key = "%02d.png".format(java.util.Locale.ROOT, index)
                    Files.copy(output.resolve(frame.key), storage.file(source.projectId, shotId, key))
                    frame.copy(key = key, timestampMs = requireNotNull(frame.timestampMs) - start)
                }
                Triple(shotId, start to end, PreparedMedia("VIDEO", end - start, copies))
            }
            return repository.structure(source, prepared)
        } finally { gate.release() }
    }

    fun frames(source: SourceFilm, segments: List<FilmSegment>): List<AnalysisFrame> {
        val frames = segments.flatMap { segment -> segment.shot.frames.map { frame ->
            val bytes = ImageContent.readNormalized(storage.file(source.projectId, segment.id, "%02d.png".format(java.util.Locale.ROOT, frame.position)), frame.width, frame.height)
            AnalysisFrame(frame.id, frame.position, segment.startMs + requireNotNull(frame.timestampMs), frame.width, frame.height, bytes)
        } }
        if (frames.size !in 1..24 || frames.sumOf { it.png.size.toLong() } > 16L * 1024 * 1024) throw AnalysisFailure("FILM_IMAGE_LIMIT", "Film Understanding exceeds the 24-frame or 16 MiB image limit.")
        return frames
    }

    fun audio(source: SourceFilm, runId: UUID): AudioInput? {
        gate.acquire()
        try {
            val input = storage.file(source.projectId, source.id, "original.bin")
            val options = listOf("-protocol_whitelist", "file", "-f", "mov", "-enable_drefs", "0", "-use_absolute_path", "0")
            val metadata = process.run(listOf(ffprobe, "-v", "error") + options + listOf("-select_streams", "a:0", "-show_entries", "stream=index", "-of", "json", input.toString()), 15)
            if (mapper.readTree(metadata).path("streams").isEmpty) return null
            val output = storage.directory(source.projectId, runId).resolve("audio.wav")
            try {
                process.run(listOf(ffmpeg, "-nostdin", "-hide_banner", "-loglevel", "error", "-xerror", "-threads", "2") + options + listOf(
                    "-i", input.toString(), "-map", "0:a:0", "-vn", "-sn", "-dn", "-t", "120", "-af", "aresample=16000:async=1:first_pts=0", "-ac", "1", "-ar", "16000", "-c:a", "pcm_s16le", "-fs", "4000000", output.toString(),
                ), 30)
                val bytes = Files.newInputStream(output).use { it.readNBytes(4_000_001) }
                if (bytes.size !in 45..4_000_000) throw AnalysisFailure("AUDIO_LIMIT", "The extracted audio exceeded its bounded PCM size.")
                return AudioInput(bytes, pcmDuration(bytes))
            } finally { Files.deleteIfExists(output) }
        } finally { gate.release() }
    }

    private fun hash(path: java.nio.file.Path): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(8192)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        return java.util.HexFormat.of().formatHex(digest.digest())
    }

    private fun videoOrigin(input: java.nio.file.Path): Long {
        val result = process.run(listOf(ffprobe, "-v", "error", "-protocol_whitelist", "file", "-f", "mov", "-enable_drefs", "0", "-use_absolute_path", "0", "-select_streams", "v:0", "-show_entries", "stream=start_time:format=start_time", "-of", "json", input.toString()), 15)
        val metadata = mapper.readTree(result)
        val streamStart = metadata.path("streams").firstOrNull()?.path("start_time")?.asString()?.toDoubleOrNull()
        val formatStart = metadata.path("format").path("start_time").asString().toDoubleOrNull()
        if (streamStart == null || formatStart == null || !streamStart.isFinite() || !formatStart.isFinite() || streamStart < formatStart) throw AnalysisFailure("SOURCE_TIMELINE_UNSUPPORTED", "This source does not expose a reliable common audio/video time origin.")
        return kotlin.math.round((streamStart - formatStart) * 1000).toLong()
    }

    internal fun pcmDuration(bytes: ByteArray): Long {
        val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        require(String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WAVE")
        var position = 12
        var formatValid = false
        while (position + 8 <= bytes.size) {
            val chunk = String(bytes, position, 4, Charsets.US_ASCII)
            val size = buffer.getInt(position + 4)
            require(size >= 0 && position.toLong() + 8 + size <= bytes.size)
            if (chunk == "fmt ") {
                require(size >= 16)
                formatValid = buffer.getShort(position + 8).toInt() == 1 && buffer.getShort(position + 10).toInt() == 1 && buffer.getInt(position + 12) == 16000 && buffer.getShort(position + 22).toInt() == 16
            }
            if (chunk == "data") {
                require(formatValid && size > 0 && size % 2 == 0)
                return (size * 1000L / 32000).also { require(it in 1..120000) }
            }
            position += 8 + size + size % 2
        }
        throw AnalysisFailure("INVALID_AUDIO", "The extracted audio has no valid PCM samples.")
    }
}
