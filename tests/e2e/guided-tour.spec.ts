import { randomUUID } from 'node:crypto';
import { expect, test, type Page } from '@playwright/test';

const preferenceKey = 'sceneproof.guided-tour.v1';
const steps = [
  { title: 'Film Intelligence', target: 'film-intelligence' },
  { title: 'Reference Bible', target: 'reference-bible' },
  { title: 'Media and timeline', target: 'media' },
  { title: 'Findings and evidence', target: 'findings' },

];

async function guardTourRequests(page: Page) {
  const unexpected: string[] = [];
  await page.route('**/api/**', async route => {
    if (!['GET', 'HEAD'].includes(route.request().method())) {
      unexpected.push(`${route.request().method()} ${new URL(route.request().url()).pathname}`);
      await route.abort();
    } else await route.continue();
  });
  return unexpected;
}

async function expectInViewport(page: Page) {
  const bounds = await page.getByRole('dialog').boundingBox();
  const viewport = page.viewportSize()!;
  expect(bounds).not.toBeNull();
  expect(bounds!.x).toBeGreaterThanOrEqual(0);
  expect(bounds!.y).toBeGreaterThanOrEqual(0);
  expect(bounds!.x + bounds!.width).toBeLessThanOrEqual(viewport.width);
  expect(bounds!.y + bounds!.height).toBeLessThanOrEqual(viewport.height);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
}

for (const width of [1280, 820, 390]) {
  test(`optional tour completes, remembers and restarts by keyboard at ${width}px without workspace writes`, async ({ page, request }) => {
    await page.setViewportSize({ width, height: 900 });
    await page.emulateMedia({ reducedMotion: 'reduce' });
    const created = await request.post('/api/v1/projects', { data: { name: `PR6 tour ${width} ${Date.now()}`, rules: 'Keep the coat.' } });
    expect(created.status()).toBe(201);
    const project = await created.json();
    const unexpected = await guardTourRequests(page);
    await page.goto(`/projects/${project.id}`);
    await expect(page.getByText('Take the quick tour')).toBeVisible();
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await page.getByRole('tab', { name: 'Continuity Findings', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'No saved findings here.' })).toBeVisible();
    await page.getByLabel('Continuity rules', { exact: true }).fill('Unsaved tour draft');
    await page.getByRole('tab', { name: 'Film Intelligence', exact: true }).click();
    const start = page.getByRole('button', { name: 'Start tour' });
    await start.focus(); await page.keyboard.press('Enter');
    const dialog = page.getByRole('dialog');
    for (const [index, step] of steps.entries()) {
      const heading = dialog.getByRole('heading', { name: `Step ${index + 1} of 4 ${step.title}` });
      await expect(heading).toBeFocused();
      await expect(dialog).toHaveAttribute('aria-modal', 'true');
      const target = page.locator(`[data-tour="${step.target}"]`);
      await expect(target).toHaveAttribute('data-tour-active', String(index + 1));
      await expect(dialog.getByText(/This area is not visible/)).toHaveCount(0);
      await expectInViewport(page);
      const targetBounds = await target.boundingBox();
      const cardBounds = await dialog.boundingBox();
      expect(targetBounds!.y).toBeLessThan(cardBounds!.y);
      await page.screenshot({ path: `test-results/tour-${width}-step-${index + 1}.png` });
      await page.keyboard.press('Tab');
      await expect(dialog.getByRole('button', { name: 'Skip' })).toBeFocused();
      await page.keyboard.press('Shift+Tab');
      await expect(dialog.getByRole('button', { name: index === 3 ? 'Done' : 'Next' })).toBeFocused();
      expect(await page.evaluate(() => getComputedStyle(document.activeElement!).outlineStyle)).not.toBe('none');
      await page.keyboard.press('Tab');
      await expect(dialog.getByRole('button', { name: 'Skip' })).toBeFocused();
      await page.keyboard.press('Tab');
      if (index > 0) {
        await expect(dialog.getByRole('button', { name: 'Back' })).toBeFocused();
        await page.keyboard.press('Tab');
      }
      await expect(dialog.getByRole('button', { name: index === 3 ? 'Done' : 'Next' })).toBeFocused();
      await page.keyboard.press('Enter');
    }
    const restart = page.getByRole('button', { name: 'Quick tour' });
    await expect(dialog).toHaveCount(0);
    await expect(restart).toBeFocused();
    await expect(page.getByLabel('Continuity rules', { exact: true })).toHaveValue('Unsaved tour draft');
    expect(await page.evaluate(key => localStorage.getItem(key), preferenceKey)).toBe('completed');
    await page.reload();
    await expect(restart).toBeVisible();
    await expect(page.getByText('Take the quick tour')).toHaveCount(0);
    await restart.focus(); await page.keyboard.press('Enter');
    await expect(dialog.getByRole('heading')).toHaveText('Step 1 of 4Film Intelligence');
    await dialog.getByRole('button', { name: 'Next' }).click();
    await dialog.getByRole('button', { name: 'Back' }).click();
    await expect(dialog.getByRole('heading')).toBeFocused();
    await page.keyboard.press('Escape');
    await expect(restart).toBeFocused();
    await page.keyboard.press('Enter');
    await dialog.getByRole('button', { name: 'Skip' }).click();
    await expect(restart).toBeFocused();
    await page.getByRole('tab', { name: 'Continuity Findings', exact: true }).click();
    await page.getByRole('button', { name: 'Refresh findings' }).click();
    await expect(page.getByRole('heading', { name: 'No saved findings here.' })).toBeVisible();
    expect(await page.evaluate(key => localStorage.getItem(key), preferenceKey)).toBe('skipped');
    expect(unexpected).toEqual([]);
    const saved = await (await request.get(`/api/v1/projects/${project.id}`)).json();
    expect(saved.rules).toBe('Keep the coat.');
  });
}

test('tour remains usable with unavailable storage and a hidden responsive anchor', async ({ page, request }) => {
  const project = await (await request.post('/api/v1/projects', { data: { name: `PR6 unavailable storage ${Date.now()}` } })).json();
  await page.addInitScript(() => {
    Object.defineProperty(window, 'localStorage', { get() { throw new DOMException('Unavailable', 'SecurityError'); } });
  });
  const unexpected = await guardTourRequests(page);
  await page.goto(`/projects/${project.id}`);
  await page.getByRole('button', { name: 'Quick tour' }).click();
  await page.addStyleTag({ content: '[data-tour="reference-bible"] { display: none; }' });
  const dialog = page.getByRole('dialog');
  await dialog.getByRole('button', { name: 'Next' }).click();
  await expect(dialog.getByText(/This area is not visible in the current layout/)).toBeVisible();
  await expect(page.locator('[data-tour-active]')).toHaveCount(0);
  await page.setViewportSize({ width: 390, height: 640 });
  await expectInViewport(page);
  await dialog.getByRole('button', { name: 'Next' }).click();
  await dialog.getByRole('button', { name: 'Next' }).click();
  await dialog.getByRole('button', { name: 'Done' }).click();
  await expect(page.getByRole('button', { name: 'Quick tour' })).toBeFocused();
  await page.reload();
  await page.getByRole('button', { name: 'Quick tour' }).click();
  await dialog.getByRole('button', { name: 'Skip' }).click();
  expect(unexpected).toEqual([]);
});

test('tour preserves selected real evidence and long Reference Bible content', async ({ page, request }) => {
  expect(await (await request.get('/__test/provider')).json()).toEqual({ provider: 'deterministic-test-only' });
  const created = await request.post('/api/v1/projects', { data: { name: 'Sequence'.repeat(15), description: 'Long project notes. '.repeat(80), rules: 'The square stays red.' } });
  expect(created.status()).toBe(201);
  const project = await created.json();
  await page.goto(`/projects/${project.id}`);
  const png = Buffer.from(await page.evaluate(() => {
    const canvas = document.createElement('canvas'); canvas.width = 320; canvas.height = 180;
    const drawing = canvas.getContext('2d')!; drawing.fillStyle = '#c9554d'; drawing.fillRect(0, 0, 320, 180);
    return canvas.toDataURL('image/png').split(',')[1]!;
  }), 'base64');
  const reference = await request.post(`/api/v1/projects/${project.id}/references`, { multipart: { title: 'Reference'.repeat(13), guidance: 'Visual continuity reference. '.repeat(60), file: { name: 'reference.png', mimeType: 'image/png', buffer: png } } });
  expect(reference.status()).toBe(201);
  for (const name of ['first.png', 'second.png']) {
    expect((await request.post(`/api/v1/projects/${project.id}/shots`, { multipart: { file: { name, mimeType: 'image/png', buffer: png } } })).status()).toBe(201);
  }
  const analyzed = await request.post(`/api/v1/projects/${project.id}/analyses`, { data: { requestId: randomUUID() } });
  expect(analyzed.status()).toBe(200);
  const run = await analyzed.json();
  const finding = (await (await request.get(`/api/v1/projects/${project.id}/findings?analysisId=${run.id}`)).json())[0];
  const unexpected = await guardTourRequests(page);
  const url = `/projects/${project.id}?analysisId=${run.id}&finding=${finding.id}`;
  await page.goto(url);
  const evidence = page.getByRole('region', { name: 'Finding evidence' });
  await expect(evidence.getByRole('img')).toHaveCount(2);
  const imageSources = await evidence.getByRole('img').evaluateAll(images => images.map(image => image.getAttribute('src')));
  for (const width of [1280, 820, 390]) {
    await page.setViewportSize({ width, height: 900 });
    await page.getByRole('button', { name: 'Quick tour' }).click();
    for (let index = 0; index < 4; index++) {
      await expectInViewport(page);
      await page.getByRole('dialog').getByRole('button', { name: index === 3 ? 'Done' : 'Next' }).click();
    }
    await expect(page).toHaveURL(new RegExp(url.replace('?', '\\?')));
    expect(await evidence.getByRole('img').evaluateAll(images => images.map(image => image.getAttribute('src')))).toEqual(imageSources);
    await expect(page.getByRole('button', { name: 'This change is intentional', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Resolve', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Dismiss', exact: true })).toBeVisible();
  }
  expect(unexpected).toEqual([]);
});
