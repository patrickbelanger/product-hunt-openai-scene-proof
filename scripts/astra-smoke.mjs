import { deflateSync } from 'node:zlib';
import { pathToFileURL } from 'node:url';
import { randomUUID } from 'node:crypto';
import { readFileSync } from 'node:fs';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';

function chunk(kind, data) {
  const payload = Buffer.concat([Buffer.from(kind), data]);
  let checksum = 0xffffffff;
  for (const value of payload) {
    checksum ^= value;
    for (let bit = 0; bit < 8; bit++) checksum = (checksum >>> 1) ^ ((checksum & 1) ? 0xedb88320 : 0);
  }
  const header = Buffer.alloc(4);
  header.writeUInt32BE(data.length);
  const trailer = Buffer.alloc(4);
  trailer.writeUInt32BE((checksum ^ 0xffffffff) >>> 0);
  return Buffer.concat([header, payload, trailer]);
}

export function fixture(color) {
  const size = 128;
  const header = Buffer.alloc(13);
  header.writeUInt32BE(size, 0);
  header.writeUInt32BE(size, 4);
  header[8] = 8;
  header[9] = 2;
  const pixels = Buffer.alloc(size * (1 + size * 3));
  for (let row = 0; row < size; row++) {
    for (let column = 0; column < size; column++) {
      const inside = row >= 32 && row < 96 && column >= 32 && column < 96;
      const offset = row * (1 + size * 3) + 1 + column * 3;
      pixels.set(inside ? color : [220, 220, 220], offset);
    }
  }
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', header), chunk('IDAT', deflateSync(pixels)), chunk('IEND', Buffer.alloc(0))]);
}

async function transportSmoke() {
  if (!process.env.OPENAI_API_KEY) throw new Error('OPENAI_API_KEY is missing.');
  const started = Date.now();
  const response = await fetch('https://api.openai.com/v1/responses', {
    method: 'POST', redirect: 'error', signal: AbortSignal.timeout(120_000),
    headers: { Authorization: `Bearer ${process.env.OPENAI_API_KEY}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      model: 'gpt-6-astra', store: false, max_output_tokens: 1200, reasoning: { effort: 'low' },
      input: [{ role: 'user', content: [
        { type: 'input_text', text: 'Describe the color and shape of the central object in this original synthetic image.' },
        { type: 'input_image', detail: 'low', image_url: `data:image/png;base64,${fixture([230, 20, 20]).toString('base64')}` },
      ] }],
      text: { format: { type: 'json_schema', name: 'sceneproof_transport_smoke', strict: true,
        schema: { type: 'object', additionalProperties: false, required: ['color', 'shape'], properties: { color: { type: 'string' }, shape: { type: 'string' } } } } },
    }),
  });
  if (!response.ok) throw new Error(`OpenAI HTTP ${response.status}; request ID ${response.headers.get('x-request-id') ?? 'unavailable'}. Provider body withheld.`);
  const result = await response.json();
  if (result.status !== 'completed') throw new Error(`OpenAI response status: ${result.status}`);
  const texts = result.output.filter(item => item.type === 'message').flatMap(item => item.content).filter(item => item.type === 'output_text');
  if (texts.length !== 1) throw new Error('Expected one structured output.');
  const parsed = JSON.parse(texts[0].text);
  if (Object.keys(parsed).sort().join(',') !== 'color,shape' || typeof parsed.color !== 'string' || typeof parsed.shape !== 'string') throw new Error('Invalid structured output.');
  if (!/red/i.test(parsed.color) || !/square/i.test(parsed.shape)) throw new Error('Vision result did not identify the fixture.');
  console.log(JSON.stringify({ stage: 'transport', model: result.model, status: result.status, imageRecognized: true, structuredOutputValidated: true, usage: result.usage, durationMs: Date.now() - started }));
}

async function applicationSmoke() {
  const base = process.env.SCENEPROOF_API_URL ?? 'http://127.0.0.1:8085';
  const origin = new URL(base);
  if (!['127.0.0.1', 'localhost', '[::1]'].includes(origin.hostname)) throw new Error('Smoke API must be local.');
  async function request(path, options = {}) {
    const response = await fetch(`${base}${path}`, { ...options, redirect: 'error', signal: AbortSignal.timeout(150_000) });
    const result = await response.json();
    if (!response.ok) throw new Error(`SceneProof HTTP ${response.status}; type ${result.type ?? 'unknown'}; analysisRunId ${result.analysisRunId ?? 'unavailable'}`);
    return result;
  }
  const project = await request('/api/v1/projects', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({
    name: `Astra original smoke ${new Date().toISOString()}`,
    description: 'Two original synthetic frames show the same square on a gray background in consecutive shots. No narrative time jump or replacement occurs.',
    rules: 'The same central square prop must stay red throughout this sequence. A color change is an unintended continuity error.',
  }) });
  const shots = [];
  for (const [index, color] of [[230, 20, 20], [20, 40, 230]].entries()) {
    const form = new FormData();
    form.append('file', new Blob([fixture(color)], { type: 'image/png' }), `original-square-${index + 1}.png`);
    shots.push(await request(`/api/v1/projects/${project.id}/shots`, { method: 'POST', body: form }));
  }
  const requestId = randomUUID();
  const started = Date.now();
  console.log(JSON.stringify({ stage: 'application-start', projectId: project.id, requestId }));
  const options = { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ requestId }) };
  const run = await request(`/api/v1/projects/${project.id}/analyses`, options);
  if (run.status !== 'SUCCEEDED') throw new Error(`Analysis ${run.id} ended ${run.status}`);
  const persisted = await request(`/api/v1/projects/${project.id}/analyses/${run.id}`);
  const replay = await request(`/api/v1/projects/${project.id}/analyses`, options);
  if (JSON.stringify(run) !== JSON.stringify(persisted) || JSON.stringify(run) !== JSON.stringify(replay)) throw new Error('Durable run or request deduplication mismatch.');
  const findings = await request(`/api/v1/projects/${project.id}/findings?analysisId=${run.id}`);
  const validator = new Ajv2020({ strict: false, allErrors: true });
  addFormats(validator);
  validator.addSchema(JSON.parse(readFileSync(new URL('../packages/api-client/openapi.json', import.meta.url), 'utf8')), 'sceneproof');
  for (const [schema, payload] of [['AnalysisRun', run], ...findings.map(finding => ['Finding', finding])]) {
    const check = validator.compile({ $ref: `sceneproof#/components/schemas/${schema}` });
    if (!check(payload)) throw new Error(`Persisted ${schema} failed OpenAPI validation.`);
  }
  const frameIds = new Set(shots.flatMap(shot => shot.frames.map(frame => frame.id)));
  const shotIds = new Set(shots.map(shot => shot.id));
  if (!findings.length || findings.some(finding => finding.analysisRunId !== run.id || finding.affectedShotIds.some(id => !shotIds.has(id)) || finding.relevantFrameIds.some(id => !frameIds.has(id)))) throw new Error('No valid persisted continuity finding.');
  console.log(JSON.stringify({ stage: 'application', projectId: project.id, analysisRunId: run.id, model: run.model, status: run.status, findingCount: findings.length, shotCount: run.shotCount, frameCount: run.frameCount, deduplicated: true, usage: run.usage, durationMs: Date.now() - started }));
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const mode = process.argv[2];
  const run = mode === '--transport' ? transportSmoke : mode === '--application' ? applicationSmoke : async () => { throw new Error('Choose --transport (one paid call) or --application (one paid analysis on a local API).'); };
  run().catch(error => { console.error(error.name === 'Error' ? error.message : `Smoke failed: ${error.name}`); process.exitCode = 1; });
}
