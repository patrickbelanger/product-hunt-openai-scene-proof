import { createHash } from 'node:crypto';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const curation = JSON.parse(readFileSync(resolve(root, 'demo/curation.json'), 'utf8'));
const source = resolve(root, 'demo', curation.source);
const hash = file => createHash('sha256').update(readFileSync(file)).digest('hex');
if (hash(source) !== curation.sourceSha256) throw new Error('Approved source hash mismatch');
const output = resolve(root, 'demo/runtime');
mkdirSync(output, { recursive: true });
function prepare(asset, reference) {
  const destination = resolve(output, asset.file);
  const argumentsList = ['-nostdin', '-hide_banner', '-loglevel', 'error', '-y', '-i', source, '-ss', String(reference ? asset.at : asset.start)];
  if (reference) argumentsList.push('-frames:v', '1');
  else argumentsList.push('-t', String(asset.end - asset.start), '-an', '-c:v', 'libx264', '-crf', '18', '-preset', 'medium', '-pix_fmt', 'yuv420p', '-movflags', '+faststart');
  argumentsList.push('-vf', reference ? 'scale=540:960' : 'scale=432:768', '-threads', '2', destination);
  execFileSync(process.env.FFMPEG_PATH || 'ffmpeg', argumentsList, { timeout: 90000, stdio: 'inherit' });
  return { file: asset.file, sha256: hash(destination), title: asset.title, guidance: asset.guidance || '' };
}
const manifest = {
  version: curation.version,
  sourceFilm: { file: 'source-film.mp4', sha256: curation.sourceSha256, title: 'Between the Line — source film' },
  project: curation.project,
  references: curation.references.map(asset => prepare(asset, true)),
  shots: curation.shots.map(asset => prepare(asset, false)),
};
writeFileSync(resolve(output, 'manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`);
console.log(`Prepared ${manifest.shots.length} clips and ${manifest.references.length} references from the approved source.`);
