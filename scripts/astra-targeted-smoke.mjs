import { randomUUID } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';

const base = process.env.SCENEPROOF_API_URL ?? 'http://127.0.0.1:8093';
const origin = new URL(base);
if (!['127.0.0.1', 'localhost', '[::1]'].includes(origin.hostname)) throw new Error('Smoke API must be local.');
const manifestPath = new URL('../.local/pr4-smoke-request.json', import.meta.url);
const resultPath = new URL('../.local/pr4-smoke-result.json', import.meta.url);

async function request(path, options = {}) {
  const response = await fetch(`${base}${path}`, { ...options, redirect: 'error', signal: AbortSignal.timeout(150_000) });
  const result = await response.json();
  if (!response.ok) throw new Error(`SceneProof HTTP ${response.status}; type ${result.type ?? 'unknown'}; analysisRunId ${result.analysisRunId ?? 'unavailable'}`);
  return result;
}

async function main() {
  await mkdir(new URL('../.local/', import.meta.url), { recursive: true });
  let manifest;
  try { manifest = JSON.parse(await readFile(manifestPath, 'utf8')); } catch (error) { if (error.code !== 'ENOENT') throw error; }
  const verify = process.argv.includes('--verify');
  if (!manifest) {
    if (verify) throw new Error('No saved smoke manifest.');
    const projectId = process.argv[process.argv.indexOf('--project') + 1];
    const findingId = process.argv[process.argv.indexOf('--finding') + 1];
    const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
    if (!uuid.test(projectId ?? '') || !uuid.test(findingId ?? '')) throw new Error('Pass --project and --finding for an existing original synthetic PR2 smoke finding.');
    const findings = await request(`/api/v1/projects/${projectId}/findings`);
    const original = findings.find(finding => finding.id === findingId);
    if (!original || original.status !== 'OPEN') throw new Error('The supplied original finding is not open on the first findings page.');
    manifest = { projectId, findingId, original, body: { requestId: randomUUID(), type: 'INTENTIONAL_CHANGE', explanation: 'The intended edit now has a time jump: the red square is repainted blue after the first shot. The earlier project description predates this creative revision. Evaluate whether this explanation coherently accounts for the difference, including any conflict with project rules.', scope: 'Only the transition between the original two synthetic square shots, after repainting during the declared time jump.', affectedShotIds: original.affectedShotIds } };
    await writeFile(manifestPath, JSON.stringify(manifest, null, 2), { flag: 'wx' });
  }
  const path = `/api/v1/projects/${manifest.projectId}/findings/${manifest.findingId}/actions`;
  const started = Date.now();
  console.log(JSON.stringify({ stage: verify ? 'verify-read-only' : 'targeted-start', projectId: manifest.projectId, findingId: manifest.findingId, requestId: manifest.body.requestId }));
  const options = { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(manifest.body) };
  const action = verify ? (await request(path)).find(item => item.requestId === manifest.body.requestId) : await request(path, options);
  if (!action || action.reanalysis?.status !== 'SUCCEEDED' || !action.result) throw new Error('No successful targeted result. Do not loop paid attempts. Inspect the saved request/history.');
  const validator = new Ajv2020({ strict: false, allErrors: true }); addFormats(validator);
  validator.addSchema(JSON.parse(await readFile(new URL('../packages/api-client/openapi.json', import.meta.url), 'utf8')), 'sceneproof');
  const check = validator.compile({ $ref: 'sceneproof#/components/schemas/FindingAction' });
  if (!check(action)) throw new Error('Saved action does not match OpenAPI.');
  const replay = verify ? action : await request(path, options);
  if (JSON.stringify(action) !== JSON.stringify(replay)) throw new Error('Replay changed the durable action.');
  const persisted = await request(`${path}/${action.id}`);
  if (JSON.stringify(action) !== JSON.stringify(persisted)) throw new Error('GET differs from the saved action.');
  const findings = await request(`/api/v1/projects/${manifest.projectId}/findings?analysisId=${manifest.original.analysisRunId}`);
  const current = findings.find(item => item.id === manifest.findingId);
  const status = action.result.outcome === 'INTENT_ACCEPTED' ? 'INTENTIONAL' : 'OPEN';
  if (JSON.stringify(current) !== JSON.stringify({ ...manifest.original, status })) throw new Error('Original finding/evidence changed unexpectedly.');
  if (action.result.projectId !== manifest.projectId || action.result.originalFindingId !== manifest.findingId || manifest.original.relevantFrameIds.some(id => !action.result.evidenceFrameIds.includes(id))) throw new Error('Evidence identity mismatch.');
  const usage = action.reanalysis.usage;
  const approximateUsd = usage?.inputTokens != null && usage?.outputTokens != null ? ((usage.inputTokens - (usage.cachedInputTokens ?? 0) - (usage.cacheWriteTokens ?? 0)) * 10 + (usage.cachedInputTokens ?? 0) + (usage.cacheWriteTokens ?? 0) * 12.5 + usage.outputTokens * 50) / 1_000_000 : null;
  const report = { stage: verify ? 'verified-read-only' : 'targeted-complete', projectId: manifest.projectId, findingId: manifest.findingId, actionId: action.id, analysisRunId: action.reanalysis.id, outcome: action.result.outcome, status, usage, approximateUsd, durationMs: Date.now() - started, deduplicated: true, originalPreserved: true, action };
  if (!verify) await writeFile(resultPath, JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report));
}

main().catch(error => { console.error(error.name === 'Error' ? error.message : `Targeted smoke failed: ${error.name}`); process.exitCode = 1; });
