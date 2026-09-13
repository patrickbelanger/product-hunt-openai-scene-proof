# Media pipeline

PR1 implements synchronous local ingestion, with no model calls.

PR5 shares those image primitives for visual references. Video bounds are unchanged.

## Formats and bounds

One multipart file per POST /api/v1/projects/{projectId}/shots. JPEG/PNG: 10 MiB,
16 million pixels, at most 8192 pixels per side. MP4/H.264 first video stream:
100 MiB, 120 seconds, 3840 by 2160, up to 60 average fps. The initial ISO-BMFF
signature check requires `ftyp` at byte offset 4, with no major-brand allowlist.
FFprobe must report an MP4/QuickTime-compatible format (`mp4` or `mov` among its
format names), a first video stream with H.264 and valid duration/dimensions/fps
within those same limits. A signature alone is insufficient. Audio is ignored.
Signatures and decoder results
determine support; client MIME and extensions do not. EXIF rotation, animated
images, HDR/color management and additional codecs are outside PR1.

Spring spools multipart to disk (100 MiB file, 101 MiB request); streaming copy
is independently bounded. One import per API process; concurrent requests get 429.
Each project allows 100 completed attempts, including failures. These are local
operational limits, not a public quota system or global disk quota.

## Storage and metadata

MediaStorage / LocalMediaStorage uses an operator-controlled root, default
.local/media relative to the API working directory. Set absolute MEDIA_ROOT for
consistent storage across Gradle, IDE and packaged-jar launches.
Layout: <root>/<project UUID>/<shot UUID>/original.bin and 00.png through 31.png.
Client names are display-only sanitized basenames, limited to 160 characters.
Generated directories, allowlisted file keys and symlink checks confine paths;
the root must not be writable by untrusted local users.

Images are re-encoded as RGB PNG up to 1600 pixels per side, stripping metadata
and flattening transparency onto black. Originals are retained but not served.
Only normalized PNG is delivered after project/frame membership lookup.
Storage keys are not exposed in the API.

V2 stores shots and frames. Decoding runs outside transactions. A short final
transaction locks the project, allocates position and atomically writes metadata.
A still has null timestamp/duration. Video timestamps are elapsed source
presentation times in milliseconds, strictly increasing and unique.
One video is one imported shot, with representative frames across its cuts.

## Controlled processes and sampling

FFMPEG_PATH and FFPROBE_PATH select executables. ProcessBuilder uses fixed argument
lists without a shell. Input options: -protocol_whitelist file -f mov
-enable_drefs 0 -use_absolute_path 0. Remote URLs, playlist demuxers and external
data references are not accepted.

FFprobe uses -select_streams v:0 and -show_entries
stream=codec_name,width,height,avg_frame_rate,duration:format=duration,format_name
-of json, with a 15-second timeout. FFmpeg uses -nostdin -xerror -threads 2,
-map 0:v:0 -an -sn -dn -t 120, one filter thread, VFR output and at most 32 frames.
With interval=duration/24 and spacing=duration/32, the filter is:

```text
setpts=PTS-STARTPTS,
select='isnan(prev_selected_t)+gte(t-prev_selected_t,spacing)*(gt(scene,0.3)+gte(t-prev_selected_t,interval))',
scale=1600:1600:force_original_aspect_ratio=decrease,
showinfo
```

The first frame, spaced cuts and periodic fallback are retained. Minimum spacing
bounds dense cuts; short clips yield fewer frames. This is representative sampling,
not exhaustive coverage. showinfo supplies actual selected timestamps relative to
the first decoded frame, not invented sample targets. Files, timestamps and counts
must agree. Extraction times out after 90 seconds. Each process has a 256 KiB
combined-output cap, bounded reader joining and forced process/descendant cleanup.
Decoder output and uploaded contents are never logged.

## Failure and recovery

Completed attempts persist READY or FAILED. Failure removes the shot directory and
records a safe code with no frames, returning an actionable Problem response.
Cleanup/database failures log only generated shot IDs; a failed database cannot
guarantee failure history. Invalid multipart, capacity, missing project and busy
rejections occur before ingestion and do not create attempts. Retry creates a new
attempt. UI pending state is real and preserves the selected file after failure.

Filesystem and PostgreSQL are not a distributed transaction. Normal failures are
compensated; abrupt termination can leave an unreferenced directory. No background
orphan collector exists. Before manual cleanup, stop the API and compare directory
shot IDs against the database. Public upload hosting still needs the pending
isolation, durable storage, disk quota and process sandbox decision.

## Reference images (PR5)

POST project `/references` accepts JPEG/PNG only, with the same 10 MiB/16 MP/8192
input and 1600 normalized bounds. Shared ImageContent supplies signature checks,
bounded copy and normalized reads; ImageNormalizer remains the single decoder.
MediaIngestionGate bounds combined shot/reference imports to one active per process.
MIME/filename claims never establish validity.

Layout: `<root>/<project UUID>/<reference UUID>/original.bin` and `00.png`.
Generated reference UUIDs are asset keys, never Shot rows. Only normalized PNG is
served, project-scoped, with bounded read/decode/dimension/hash checks and nosniff.
Archived images remain available; no arbitrary URL or client storage key is accepted.

Normal failures remove the generated directory and create no reference row; failed
reference attempts have no history table. Abrupt termination can leave an orphan as
in PR1. Eight active and 100 successfully persisted references bound the local Bible,
not global/public disk usage. No historical file deletion. Analysis references and
frames share 8 MiB/image and 16 MiB aggregate; selected missing/corrupt content fails
the entire request before provider submission.

## Verification

Tests use PostgreSQL, ImageIO and real FFmpeg-generated original color fixtures:
PNG/JPEG pixels, ordering/timestamps, project scoping, corrupt/oversize images,
unsupported codecs, duration, traversal keys, timeout, missing tools and output cap.
Regression fixtures also cover an `iso6` major brand confirmed by real FFprobe
and a corrupt file containing only an `ftyp` signature.
Browser checks cover multipart, contract, decoded image load, reload, failed history
and tablet overflow. Current executed results are in STATUS.

References: [FFmpeg filters](https://ffmpeg.org/ffmpeg-filters.html),
[FFprobe](https://ffmpeg.org/ffprobe.html),
[ADR-0003](adr/ADR-0003-local-media-ingestion.md).
