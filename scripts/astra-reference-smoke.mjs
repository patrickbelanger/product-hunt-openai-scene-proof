import { randomUUID, createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { fixture } from './astra-smoke.mjs';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';

const base = process.env.SCENEPROOF_API_URL ?? 'http://127.0.0.1:8095';
if (!['127.0.0.1', 'localhost', '[::1]'].includes(new URL(base).hostname)) throw new Error('Smoke API must be local.');
const manifestPath = new URL('../.local/pr5-smoke-request.json', import.meta.url);
const verifyOnly = process.argv.includes('--verify');
const validator = new Ajv2020({ strict: false, allErrors: true });
addFormats(validator);
validator.addSchema(JSON.parse(readFileSync(new URL('../packages/api-client/openapi.json', import.meta.url), 'utf8')), 'sceneproof');

async function request(path, options = {}) {
  const response = await fetch(`${base}${path}`, { ...options, redirect: 'error', signal: AbortSignal.timeout(150_000) });
  const result = await response.json();
  if (!response.ok) throw new Error(`SceneProof HTTP ${response.status}; type ${result.type ?? 'unknown'}; analysisRunId ${result.analysisRunId ?? 'unavailable'}`);
  return result;
}
const post = body => ({ method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
function validate(schema, value) {
  const check = validator.compile({ $ref: `sceneproof#/components/schemas/${schema}` });
  if (!check(value)) throw new Error(`${schema} violates OpenAPI.`);
}

async function main() {
  let manifest;
  if (existsSync(manifestPath)) manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
  else {
    if (verifyOnly) throw new Error('No PR5 smoke manifest exists.');
    const project = await request('/api/v1/projects', post({ name: `PR5 original visual reference smoke ${new Date().toISOString()}`, description: 'Two consecutive views of the same central square prop against gray. The creator declares its appearance using a reference image. No replacement or repainting occurs between views.', rules: '' }));
    const form = new FormData();
    form.append('file', new Blob([fixture([230, 20, 20])], { type: 'image/png' }), 'declared-red-square.png');
    form.append('title', 'Canonical red square prop');
    form.append('guidance', 'This is the intended appearance of the central square prop: red, with the same shape. Keep its identity and color consistent; camera and lighting differences still need contextual judgement.');
    const reference = await request(`/api/v1/projects/${project.id}/references`, { method: 'POST', body: form });
    validate('Reference', reference);
    const shots = [];
    for (const name of ['observed-square-first.png', 'observed-square-second.png']) {
      const upload = new FormData();
      upload.append('file', new Blob([fixture([20, 40, 230])], { type: 'image/png' }), name);
      shots.push(await request(`/api/v1/projects/${project.id}/shots`, { method: 'POST', body: upload }));
    }
    manifest = { projectId: project.id, reference, shots, requestId: randomUUID() };
    mkdirSync(new URL('../.local/', import.meta.url), { recursive: true });
    writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));
  }
  const started = Date.now();
  let run;
  if (verifyOnly) {
    if (!manifest.runId) throw new Error('No saved run in manifest; inspect the recorded request before further work.');
    run = await request(`/api/v1/projects/${manifest.projectId}/analyses/${manifest.runId}`);
  } else {
    console.log(JSON.stringify({ stage: 'PR5-start', projectId: manifest.projectId, referenceId: manifest.reference.id, requestId: manifest.requestId }));
    run = await request(`/api/v1/projects/${manifest.projectId}/analyses`, post({ requestId: manifest.requestId }));
    manifest.runId = run.id;
    writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));
    const replay = await request(`/api/v1/projects/${manifest.projectId}/analyses`, post({ requestId: manifest.requestId }));
    if (JSON.stringify(run) !== JSON.stringify(replay)) throw new Error('Request replay changed the saved run.');
  }
  validate('AnalysisRun', run);
  if (run.status !== 'SUCCEEDED') throw new Error(`Run ${run.id} is ${run.status}; do not create another request to force a finding.`);
  const findings = await request(`/api/v1/projects/${manifest.projectId}/findings?analysisId=${run.id}`);
  const shotIds = new Set(manifest.shots.map(shot => shot.id));
  const frameIds = new Set(manifest.shots.flatMap(shot => shot.frames.map(frame => frame.id)));
  for (const finding of findings) {
    validate('Finding', finding);
    if (finding.relevantReferenceIds.some(id => id !== manifest.reference.id) || finding.affectedShotIds.some(id => !shotIds.has(id)) || finding.relevantFrameIds.some(id => !frameIds.has(id))) throw new Error('Unexpected evidence identity.');
    const references = await request(`/api/v1/projects/${manifest.projectId}/findings/${finding.id}/references`);
    if (references.length !== finding.relevantReferenceIds.length || references.some(reference => reference.sha256 !== manifest.reference.sha256 || reference.guidance !== manifest.reference.guidance)) throw new Error('Historical reference association mismatch.');
  }
  const content = await fetch(`${base}${manifest.reference.url}`, { redirect: 'error' });
  if (!content.ok || createHash('sha256').update(Buffer.from(await content.arrayBuffer())).digest('hex') !== manifest.reference.sha256) throw new Error('Original normalized reference hash mismatch.');
  console.log(JSON.stringify({ stage: verifyOnly ? 'PR5-restart-read' : 'PR5-result', projectId: manifest.projectId, runId: run.id, referenceId: manifest.reference.id, findingIds: findings.map(finding => finding.id), cited: findings.some(finding => finding.relevantReferenceIds.includes(manifest.reference.id)), model: run.model, usage: run.usage, durationMs: Date.now() - started, replayChecked: !verifyOnly }));
}
main().catch(error => { console.error(error.name === 'Error' ? error.message : `Smoke failed: ${error.name}`); process.exitCode = 1; });
