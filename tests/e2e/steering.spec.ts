import { randomUUID } from 'node:crypto';
import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';
import spec from '../../packages/api-client/openapi.json';
import type { Finding, FindingAction } from '@sceneproof/api-client';

async function fixture(page: Page, request: APIRequestContext) {
  const marker = await request.get('/__test/provider');
  expect(marker.ok(), 'Use the isolated deterministic browser test server, never a paid provider.').toBe(true);
  expect(await marker.json()).toEqual({ provider: 'deterministic-test-only' });
  const created = await request.post('/api/v1/projects', { data: { name: `PR4 steering verification ${Date.now()}`, rules: 'The square stays red unless a narrative transition explains its change.' } });
  expect(created.status()).toBe(201);
  const project = await created.json();
  await page.goto(`/projects/${project.id}`);
  for (const [index, color] of ['#c9554d', '#527bc4'].entries()) {
    const png = await page.evaluate(fill => { const canvas = document.createElement('canvas'); canvas.width = 640; canvas.height = 360; const drawing = canvas.getContext('2d')!; drawing.fillStyle = '#17201e'; drawing.fillRect(0, 0, 640, 360); drawing.fillStyle = fill; drawing.fillRect(220, 80, 200, 200); return canvas.toDataURL('image/png').split(',')[1]!; }, color);
    const imported = await request.post(`/api/v1/projects/${project.id}/shots`, { multipart: { file: { name: `intent-square-${index + 1}.png`, mimeType: 'image/png', buffer: Buffer.from(png, 'base64') } } });
    expect(imported.status()).toBe(201);
  }
  const analyzed = await request.post(`/api/v1/projects/${project.id}/analyses`, { data: { requestId: randomUUID() } });
  expect(analyzed.status()).toBe(200);
  const findings: Finding[] = await (await request.get(`/api/v1/projects/${project.id}/findings`)).json();
  const finding = findings[0]!;
  await page.goto(`/projects/${project.id}?analysisId=${finding.analysisRunId}&finding=${finding.id}`);
  await expect(page.getByRole('heading', { name: 'Finding evidence' })).toBeVisible();
  return { project, finding, path: `/api/v1/projects/${project.id}/findings/${finding.id}/actions` };
}

for (const remains of [false, true]) test(`intent → ${remains ? 'issue remains then dismiss' : 'accepted intent'} → reload preserves history and evidence`, async ({ page, request }) => {
  const { project, finding, path } = await fixture(page, request);
  const writes: string[] = [];
  page.on('request', outgoing => { if (outgoing.method() !== 'GET' && outgoing.url().includes('/api/')) writes.push(outgoing.url()); });
  const intent = page.getByRole('button', { name: 'This change is intentional' });
  await expect(intent).toBeEnabled();
  await intent.focus(); await page.keyboard.press('Enter');
  const explanation = remains ? 'The issue remains because the repaint occurs after this sequence.' : 'The square is repainted after the character arrives home between shots.';
  await page.getByLabel('Why is this change intentional?').fill(explanation);
  await page.getByLabel('Narrative scope').fill('The time jump between shots 1 and 2.');
  const responsePromise = page.waitForResponse(response => response.url().endsWith(path) && response.request().method() === 'POST');
  await page.getByRole('button', { name: 'Confirm intent & re-evaluate' }).focus(); await page.keyboard.press('Enter');
  const response = await responsePromise;
  expect(response.status()).toBe(200);
  const saved: FindingAction = await response.json();
  expect(saved.result?.outcome).toBe(remains ? 'ISSUE_REMAINS' : 'INTENT_ACCEPTED');
  expect(saved.reanalysis?.kind).toBe('TARGETED');
  const validator = new Ajv2020({ strict: false, allErrors: true }); addFormats(validator); validator.addSchema(spec, 'sceneproof');
  const check = validator.compile({ $ref: 'sceneproof#/components/schemas/FindingAction' });
  expect(check(saved), JSON.stringify(check.errors)).toBe(true);
  const judgement = page.getByRole('alert', { name: remains ? 'The continuity issue remains' : 'Intent explains the difference' });
  await expect(judgement).toBeVisible();
  await expect(page.getByText(/saved findings on this page/)).toContainText(remains ? '1 open' : '0 open');
  expect(writes).toHaveLength(1);
  const replay = await request.post(path, { data: response.request().postDataJSON() });
  expect(await replay.json()).toEqual(saved);
  await page.reload(); await expect(judgement).toBeVisible();
  await page.getByText(/View immutable history/).click();
  await expect(page.getByText(`Creator: ${explanation}`)).toBeVisible();
  await expect(page.getByText(finding.explanation, { exact: true })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Finding evidence' }).getByRole('img').first()).toHaveJSProperty('naturalWidth', 640);
  await page.getByRole('button', { name: 'Reload saved findings' }).click();
  await page.getByRole('button', { name: 'Refresh history' }).click();
  expect(writes).toHaveLength(1);
  const persisted: Finding[] = await (await request.get(`/api/v1/projects/${project.id}/findings`)).json();
  expect(persisted[0]).toEqual({ ...finding, status: remains ? 'OPEN' : 'INTENTIONAL' });
  await page.setViewportSize({ width: 820, height: 1180 });
  await page.screenshot({ path: `test-results/steering-${remains ? 'remains' : 'accepted'}-tablet.png`, fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  if (remains) {
    await page.getByRole('button', { name: 'Dismiss', exact: true }).click();
    await page.getByLabel('Creator note').fill('The creator chooses to keep this artistic discrepancy.');
    await page.getByRole('button', { name: 'Confirm dismiss' }).click();
    await expect(page.getByText('DISMISSED', { exact: true })).toBeVisible();
    await page.reload(); await expect(page.getByText('DISMISSED', { exact: true })).toBeVisible();
    const history: FindingAction[] = await (await request.get(path)).json();
    expect(history).toHaveLength(2); expect(history[1]!.supersedesActionId).toBe(saved.id); expect(history[1]!.reanalysis).toBeNull();
    expect(writes).toHaveLength(2);
  }
});

test('resolve records creator correction without a targeted provider run', async ({ page, request }) => {
  const { path } = await fixture(page, request);
  await page.getByRole('button', { name: 'Resolve', exact: true }).click();
  await page.getByLabel('Creator note').fill('I corrected the prop in the final edit.');
  await page.getByRole('button', { name: 'Confirm resolve' }).click();
  await expect(page.getByText('RESOLVED', { exact: true })).toBeVisible();
  await page.reload(); await expect(page.getByText('RESOLVED', { exact: true })).toBeVisible();
  const history: FindingAction[] = await (await request.get(path)).json();
  expect(history).toHaveLength(1); expect(history[0]!.type).toBe('RESOLVE'); expect(history[0]!.reanalysis).toBeNull(); expect(history[0]!.result).toBeNull();
});
