# Media pipeline

Status: planned; multipart upload is explicitly disabled in foundation. No media
endpoint, storage adapter or FFmpeg execution exists in the API yet.

Next branch: `feat/p0-media-ingestion`. Implement image and short-video ingestion
before Astra. FFmpeg and FFprobe are local infrastructure dependencies, not libraries
to copy from another product.

Initial candidate limits for validation in that slice: JPEG/PNG stills ≤10 MiB;
MP4/H.264 video ≤100 MiB and ≤120 seconds; at most 32 representative frames per
ingestion. These are proposed bounds, not currently accepted upload capabilities.
Check actual signatures/decoder results, dimensions, duration and count. Reject
unsupported/corrupt inputs before analysis; do not trust extension or client MIME.

Use generated UUID storage keys under an isolated project directory behind
MediaStorage. Originals and normalized frames stay out of PostgreSQL; metadata,
order and source timestamps are relational. Never resolve client paths or fetch
untrusted remote media URLs. Enforce canonical paths and decoder resource limits.

Invoke ProcessBuilder with an executable and fixed argument list, no shell and no
raw user fragments. Enforce timeout, process cleanup, bounded output and temporary
directory cleanup on failure. Inspect first, combine scene-change extraction and
bounded periodic fallback, deduplicate timestamps and sort frames deterministically.
Preserve source timestamps; do not invent timecodes for still images.

Document exact extraction arguments, thresholds, supported codecs, failure stages,
cleanup ownership and tests when implemented. Storage failures, malformed input,
decoder timeout and zero-frame output must produce explicit failed ingestion state.
