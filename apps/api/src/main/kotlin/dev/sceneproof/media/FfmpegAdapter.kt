package dev.sceneproof.media

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.math.roundToLong

@Component
class FfmpegAdapter(
    private val process: MediaProcess,
    private val mapper: ObjectMapper,
    private val images: ImageNormalizer,
    @param:Value("\${sceneproof.media.ffmpeg:ffmpeg}") private val ffmpeg: String,
    @param:Value("\${sceneproof.media.ffprobe:ffprobe}") private val ffprobe: String,
) {
    fun extract(input: Path): PreparedMedia {
        val inputOptions = listOf("-protocol_whitelist", "file", "-f", "mov", "-enable_drefs", "0", "-use_absolute_path", "0")
        val metadata = process.run(listOf(ffprobe, "-v", "error") + inputOptions + listOf(
            "-select_streams", "v:0", "-show_entries", "stream=codec_name,width,height,avg_frame_rate,duration:format=duration,format_name", "-of", "json", input.toString(),
        ), 15)
        val root = try { mapper.readTree(metadata) } catch (_: Exception) { throw MediaFailure("INVALID_VIDEO", "The video metadata is invalid.") }
        val stream = root.path("streams").firstOrNull() ?: throw MediaFailure("INVALID_VIDEO", "The file has no video stream.")
        val formats = root.path("format").path("format_name").asString().split(',')
        if (formats.none { it == "mov" || it == "mp4" }) {
            throw MediaFailure("INVALID_VIDEO", "Use a video in an MP4/QuickTime-compatible container.")
        }
        val duration = root.path("format").path("duration").asString().toDoubleOrNull() ?: Double.NaN
        val rate = stream.path("avg_frame_rate").asString().split('/').map { it.toDoubleOrNull() ?: Double.NaN }
        val fps = if (rate.size == 2) rate[0] / rate[1] else Double.NaN
        if (stream.path("codec_name").asString() != "h264" || !duration.isFinite() || duration <= 0 || duration > 120 ||
            stream.path("width").asInt() !in 1..3840 || stream.path("height").asInt() !in 1..2160 || !fps.isFinite() || fps <= 0 || fps > 60) {
            throw MediaFailure("VIDEO_LIMITS", "Use MP4/H.264 video up to 120 seconds, 3840 by 2160 pixels and 60 fps.")
        }
        val interval = String.format(Locale.ROOT, "%.6f", duration / 24)
        val spacing = String.format(Locale.ROOT, "%.6f", duration / 32)
        val filter = "setpts=PTS-STARTPTS,select='isnan(prev_selected_t)+gte(t-prev_selected_t,$spacing)*(gt(scene,0.3)+gte(t-prev_selected_t,$interval))',scale=1600:1600:force_original_aspect_ratio=decrease,showinfo"
        val result = process.run(listOf(ffmpeg, "-nostdin", "-hide_banner", "-loglevel", "info", "-xerror", "-threads", "2") + inputOptions + listOf(
            "-i", input.toString(), "-map", "0:v:0", "-an", "-sn", "-dn", "-t", "120", "-vf", filter,
            "-filter_threads", "1", "-fps_mode", "vfr", "-frames:v", "32", "-threads", "2", "-start_number", "0", input.parent.resolve("%02d.png").toString(),
        ), 90)
        val times = Regex("\\bn:\\s*\\d+\\s+pts:.*?pts_time:([0-9.eE+-]+)").findAll(result)
            .map { (it.groupValues[1].toDouble() * 1000).roundToLong() }.toList()
        val files = Files.list(input.parent).use { entries -> entries.filter { it.fileName.toString().matches(Regex("[0-9]{2}\\.png")) }.sorted().toList() }
        if (times.isEmpty() || times.size != files.size || times.size > 32 || times.any { it < 0 || it > (duration * 1000).roundToLong() } || times.zipWithNext().any { it.first >= it.second }) {
            throw MediaFailure("INVALID_FRAMES", "No consistent frames could be extracted from this video.")
        }
        val frames = files.mapIndexed { index, file ->
            val temporary = file.resolveSibling("normalized.png")
            val frame = images.normalize(file, temporary, times[index])
            Files.move(temporary, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            frame.copy(key = file.fileName.toString())
        }
        return PreparedMedia("VIDEO", (duration * 1000).roundToLong(), frames)
    }
}
