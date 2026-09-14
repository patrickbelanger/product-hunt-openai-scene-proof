import { randomUUID, createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';

const base = process.env.SCENEPROOF_API_URL ?? 'http://127.0.0.1:8098';
if (!['127.0.0.1', 'localhost', '[::1]'].includes(new URL(base).hostname)) throw new Error('Validation API must be local.');
const verifyOnly = process.argv.includes('--verify');
if (!verifyOnly && !process.argv.includes('--run')) throw new Error('Use --run for the one authorized full analysis, or --verify for GET-only verification.');
const manifestPath = new URL('../.local/pr7-validation-request.json', import.meta.url);
const resultPath = new URL('../.local/pr7-validation-result.json', import.meta.url);
const validator = new Ajv2020({ strict: false, allErrors: true }); addFormats(validator);
validator.addSchema(JSON.parse(readFileSync(new URL('../packages/api-client/openapi.json', import.meta.url), 'utf8')), 'sceneproof');
const post = body => ({ method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
async function request(path, options = {}) {
  const response = await fetch(`${base}${path}`, { ...options, redirect: 'error', signal: AbortSignal.timeout(190000) });
  const body = await response.json();
  if (!response.ok) throw Object.assign(new Error(`SceneProof HTTP ${response.status}; ${body.type ?? 'unknown failure'}`), { runId: body.analysisRunId });
  return body;
}
function validate(schema, value) {
  const check = validator.compile({ $ref: `sceneproof#/components/schemas/${schema}` });
  if (!check(value)) throw new Error(`${schema} violates OpenAPI.`);
}
async function main() {
  mkdirSync(new URL('../.local/', import.meta.url), { recursive: true });
  const manifest = existsSync(manifestPath) ? JSON.parse(readFileSync(manifestPath, 'utf8')) : { demoRequestId: randomUUID(), analysisRequestId: randomUUID(), attempted: false };
  const save = () => writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
  let run;
  if (verifyOnly) {
    if (!manifest.runId || !manifest.projectId) throw new Error('No recorded run to verify. Inspect the existing request; do not allocate another paid request.');
    run = await request(`/api/v1/projects/${manifest.projectId}/analyses/${manifest.runId}`);
  } else {
    if (manifest.attempted) throw new Error('PR7 analysis already attempted. Use --verify; no reroll is permitted.');
    save();
    const marker = await fetch(`${base}/__test/provider`, { redirect: 'error' });
    if (marker.ok) throw new Error('Use the normal application for real validation, not a test provider.');
    const preparationStarted = Date.now();
    const project = await request('/api/v1/demo', post({ requestId: manifest.demoRequestId }));
    manifest.preparationMs = Date.now() - preparationStarted;
    validate('Project', project);
    if (project.demo?.templateVersion !== 'between-the-line-v1') throw new Error('Unexpected demo version.');
    manifest.projectId = project.id; save();
    const shots = await request(`/api/v1/projects/${project.id}/shots`);
    const references = await request(`/api/v1/projects/${project.id}/references`);
    if (shots.length !== 8 || references.length !== 5 || shots.some(shot => shot.status !== 'READY')) throw new Error('Incomplete demo.');
    const selected = shots.flatMap(shot => [...new Set([0, Math.floor((shot.frames.length - 1) / 2), shot.frames.length - 1])].map(position => shot.frames[position]));
    let imageBytes = 0;
    for (const image of [...selected, ...references]) {
      const response = await fetch(`${base}${image.url}`, { redirect: 'error' });
      if (!response.ok) throw new Error('Image unavailable before validation.');
      const content = Buffer.from(await response.arrayBuffer()); imageBytes += content.length;
      if (content.length > 8 * 1024 * 1024) throw new Error('Oversized image.');
      if (image.sha256 && createHash('sha256').update(content).digest('hex') !== image.sha256) throw new Error('Reference hash mismatch.');
    }
    if (imageBytes > 16 * 1024 * 1024) throw new Error('Demo exceeds the unchanged image budget.');
    manifest.selectedFrames = selected.map(frame => frame.id); manifest.imageBytes = imageBytes;
    manifest.attempted = true; manifest.startedAt = new Date().toISOString(); save();
    try {
      run = await request(`/api/v1/projects/${project.id}/analyses`, post({ requestId: manifest.analysisRequestId }));
      manifest.runId = run.id; save();
    } catch (failure) {
      manifest.runId = failure.runId ?? null; manifest.failure = failure.message; save();
      if (!manifest.runId) throw failure;
      run = await request(`/api/v1/projects/${project.id}/analyses/${manifest.runId}`);
    }
  }
  validate('AnalysisRun', run);
  const shots = await request(`/api/v1/projects/${manifest.projectId}/shots`);
  const references = await request(`/api/v1/projects/${manifest.projectId}/references`);
  const findings = await request(`/api/v1/projects/${manifest.projectId}/findings?analysisId=${run.id}`);
  for (const finding of findings) {
    validate('Finding', finding);
    const cited = await request(`/api/v1/projects/${manifest.projectId}/findings/${finding.id}/references`);
    if (cited.length !== finding.relevantReferenceIds.length || cited.some(reference => !references.some(original => original.id === reference.id && original.sha256 === reference.sha256))) throw new Error('Reference evidence association mismatch.');
  }
  const result = { templateVersion: 'between-the-line-v1', run, shots, references, findings, imageBytes: manifest.imageBytes, preparationMs: manifest.preparationMs, verifiedAt: new Date().toISOString(), verificationMode: verifyOnly ? 'GET-only' : 'single-authorized-analysis' };
  writeFileSync(resultPath, `${JSON.stringify(result, null, 2)}\n`);
  console.log(JSON.stringify({ projectId: manifest.projectId, runId: run.id, status: run.status, failureCode: run.failureCode, model: run.model, usage: run.usage, durationMs: run.completedAt ? Date.parse(run.completedAt) - Date.parse(run.startedAt) : null, imageBytes: manifest.imageBytes, findings: findings.map(finding => ({ id: finding.id, category: finding.category, title: finding.title })) }));
  if (run.status !== 'SUCCEEDED') throw new Error('The one validation did not succeed. Preserve its result; do not reroll.');
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
