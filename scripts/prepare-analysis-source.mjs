import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
export const sourceProvenance = {
  masterPath: 'demo/between the lines - demo.mp4',
  masterFile: 'source-film.mp4',
  masterSha256: 'ac9b29c47eeb399dbd1ac5cdfcde19273e0e4e68f5df8fd67fedbef3d284a7a7',
  masterDurationUs: 92458667,
  sourceStartUs: 0,
  sourceEndUs: 36291667,
};
const hash = path => createHash('sha256').update(readFileSync(path)).digest('hex');

export function prepareAnalysisSource(destination = resolve(root, 'demo/runtime/analysis-source.mp4')) {
  const master = resolve(root, sourceProvenance.masterPath);
  if (resolve(destination).toLowerCase() === master.toLowerCase() || hash(master) !== sourceProvenance.masterSha256 || (existsSync(destination) && hash(destination) === sourceProvenance.masterSha256)) throw new Error('Immutable master check failed');
  const metadata = JSON.parse(execFileSync(process.env.FFPROBE_PATH || 'ffprobe', ['-v', 'error', '-show_streams', '-show_format', '-of', 'json', master], { encoding: 'utf8' }));
  const picture = metadata.streams.find(stream => stream.codec_type === 'video');
  const audio = metadata.streams.find(stream => stream.codec_type === 'audio');
  if (picture?.r_frame_rate !== '24/1' || audio?.sample_rate !== '48000' || Number(picture.start_time) !== 0 || Number(audio.start_time) !== 0 || Math.abs(Number(metadata.format.duration) - 92.458667) > 0.000001) throw new Error('Approved master timing changed');
  mkdirSync(dirname(destination), { recursive: true });
  execFileSync(process.env.FFMPEG_PATH || 'ffmpeg', [
    '-nostdin', '-hide_banner', '-loglevel', 'error', '-xerror', '-y',
    '-protocol_whitelist', 'file', '-f', 'mov', '-enable_drefs', '0', '-use_absolute_path', '0', '-i', master,
    '-map', '0:v:0', '-map', '0:a:0', '-map_metadata', '-1', '-map_chapters', '-1',
    '-vf', 'trim=start_frame=0:end_frame=871,setpts=PTS-STARTPTS',
    '-af', 'atrim=start_sample=0:end_sample=1742000,asetpts=PTS-STARTPTS',
    '-c:v', 'libx264', '-crf', '18', '-preset', 'medium', '-pix_fmt', 'yuv420p', '-threads:v', '2',
    '-c:a', 'alac', '-ar', '48000', '-threads:a', '1', '-fflags', '+bitexact',
    '-flags:v', '+bitexact', '-flags:a', '+bitexact', '-video_track_timescale', '24000', '-movflags', '+faststart', destination,
  ], { timeout: 180000, stdio: 'inherit' });
  if (hash(master) !== sourceProvenance.masterSha256) throw new Error('Master changed during derivation');
  return { file: 'analysis-source.mp4', sha256: hash(destination), title: 'Between the Line — Film Intelligence source' };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  console.log(JSON.stringify({ sourceFilm: prepareAnalysisSource(process.argv[2] ? resolve(process.argv[2]) : undefined), sourceProvenance }));
}
