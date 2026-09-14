import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { setTimeout as delay } from 'node:timers/promises';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';

const base = process.env.SCENEPROOF_API_URL ?? 'http://127.0.0.1:8109';
if (!['127.0.0.1', 'localhost', '[::1]'].includes(new URL(base).hostname)) throw new Error('Validation is restricted to the local application.');
const verifyOnly = process.argv.includes('--verify');
const derivedSource = process.argv.includes('--derived-source');
if (!verifyOnly && !process.argv.includes('--execute-paid')) throw new Error('Use --verify for GET-only recovery. --execute-paid requires prior green deterministic tests/builds and Patrick’s one-attempt authorization.');
const prefix = derivedSource ? 'pr8-derived-film' : 'pr8-film';
const manifestPath = new URL(`../.local/${prefix}-attempt.json`, import.meta.url);
const resultPath = new URL(`../.local/${prefix}-result.json`, import.meta.url);
const lockPath = new URL(`../.local/${prefix}-paid-attempt.lock`, import.meta.url);
const validator = new Ajv2020({ strict: false, allErrors: true });
addFormats(validator);
validator.addSchema(JSON.parse(readFileSync(new URL('../packages/api-client/openapi.json', import.meta.url))), 'sceneproof');
const post = body => ({ method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
async function request(path, options = {}) {
  const response = await fetch(`${base}${path}`, { ...options, redirect: 'error', signal: AbortSignal.timeout(190000) });
  if (!response.ok) throw new Error(`SceneProof HTTP ${response.status}; no request was automatically replayed.`);
  return response.json();
}
function validate(schema, value) {
  if (!validator.compile({ $ref: `sceneproof#/components/schemas/${schema}` })(value)) throw new Error(`${schema} violates OpenAPI.`);
}
async function main() {
  mkdirSync(new URL('../.local/', import.meta.url), { recursive: true });
  let manifest;
  if (verifyOnly) {
    if (!existsSync(manifestPath)) throw new Error('No recorded PR8 attempt. Do not allocate a recovery request.');
    manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
  } else {
    if (existsSync(manifestPath)) throw new Error('PR8 attempt already recorded. Use --verify, never reroll.');
    const git = (...args) => execFileSync('git', ['-c', 'safe.directory=C:/Data/Workspace/Kotlin/product-hunt-openai-scene-proof', ...args], { encoding: 'utf8' }).trim();
    if (git('status', '--porcelain') || git('branch', '--show-current') !== 'feat/p1-ai-film-understanding') throw new Error('Validation requires a clean PR8 worktree.');
    const head = git('rev-parse', 'HEAD');
    writeFileSync(lockPath, 'One authorized PR8 attempt. Never remove to reroll.\n', { flag: 'wx' });
    manifest = { head, demoRequestId: randomUUID(), requestId: randomUUID(), attempted: false };
    writeFileSync(manifestPath, JSON.stringify(manifest, null, 2), { flag: 'wx' });
    const marker = await fetch(`${base}/__test/provider`, { redirect: 'error', signal: AbortSignal.timeout(10000) });
    if (marker.ok) throw new Error('Real validation cannot use a deterministic test provider.');
    const project = await request('/api/v1/demo', post({ requestId: manifest.demoRequestId }));
    validate('Project', project);
    manifest.projectId = project.id;
    const intelligence = await request(`/api/v1/projects/${project.id}/film`);
    validate('FilmIntelligence', intelligence);
    const template = JSON.parse(readFileSync(new URL('../demo/runtime/manifest.json', import.meta.url), 'utf8'));
    const expectedHash = derivedSource ? template.sourceFilm.sha256 : 'ac9b29c47eeb399dbd1ac5cdfcde19273e0e4e68f5df8fd67fedbef3d284a7a7';
    if (intelligence.runs.length || intelligence.source?.sha256 !== expectedHash) throw new Error('Validation requires its approved source with no existing film attempt.');
    if (derivedSource && (intelligence.source.durationMs < 36291 || intelligence.source.durationMs > 36293 || template.sourceProvenance.sourceStartUs !== 0 || template.sourceProvenance.sourceEndUs !== 36291667)) throw new Error('Derived source preflight failed; no paid dispatch.');
    manifest.sourceSha256 = intelligence.source.sha256;
    manifest.sourceDurationMs = intelligence.source.durationMs;
    if (derivedSource) manifest.sourceProvenance = template.sourceProvenance;
    console.log(JSON.stringify({ masterDurationSeconds: 92.458667, analysisSourceDurationMs: intelligence.source.durationMs, analysisSourceSha256: intelligence.source.sha256, transcriptionEndpoint: 'https://api.openai.com/v1/audio/transcriptions', transcriptionModel: 'gpt-4o-transcribe-diarize', chatOrResponsesTranscription: false, astraOnlyAfterValidatedTranscript: true }));
    manifest.sourceFilmId = intelligence.source.id;
    manifest.startedAt = new Date().toISOString(); manifest.attempted = true;
    writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
    const run = await request(`/api/v1/projects/${project.id}/film/runs`, post({ requestId: manifest.requestId, sourceFilmId: manifest.sourceFilmId, paidConsent: true }));
    validate('FilmRun', run); manifest.runId = run.id;
    writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
  }
  if (!manifest.projectId) throw new Error('Preparation was interrupted; preserve the manifest and inspect manually.');
  let intelligence;
  let run;
  for (let attempt = 0; attempt < 330; attempt++) {
    intelligence = await request(`/api/v1/projects/${manifest.projectId}/film`);
    validate('FilmIntelligence', intelligence);
    run = intelligence.runs.find(value => value.requestId === manifest.requestId);
    if (!run) throw new Error('No server acknowledgement exists. This script will not resend a paid POST.');
    if (run.completedAt) break;
    await delay(2000);
  }
  if (!run?.completedAt) throw new Error('Run remains active; use GET-only --verify later.');
  const transcript = await request(`/api/v1/projects/${manifest.projectId}/film/runs/${run.id}/transcript`);
  const candidates = await request(`/api/v1/projects/${manifest.projectId}/film/runs/${run.id}/candidates`);
  transcript.forEach(value => validate('TranscriptSegment', value)); candidates.forEach(value => validate('FilmCandidate', value));
  writeFileSync(resultPath, `${JSON.stringify({ intelligence, run, transcript, candidates, verifiedAt: new Date().toISOString(), mode: verifyOnly ? 'GET-only' : 'single-authorized-attempt' }, null, 2)}\n`);
  console.log(JSON.stringify({ projectId: manifest.projectId, runId: run.id, stage: run.stage, failureCode: run.failureCode, failureMessage: run.failureMessage, transcriptionModel: run.transcriptionModel, transcriptionRequestId: run.transcriptionRequestId, providerRequestId: run.providerRequestId, audioStatus: run.audioStatus, audioDurationMs: run.audioDurationMs, transcriptSegments: transcript.length, candidates: candidates.length, usage: run.usage, durationMs: Date.parse(run.completedAt) - Date.parse(run.startedAt) }));
  if (run.stage !== 'SUCCEEDED') throw new Error('The one authorized run failed. Preserve it; no reroll.');
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
