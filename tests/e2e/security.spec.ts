import { expect, test } from '@playwright/test';

test('browser and API security headers preserve same-origin project use and safe rejection', async ({ page, request }) => {
  const marker = await request.get('/__test/provider');
  expect(await marker.json()).toEqual({ provider: 'deterministic-test-only' });
  const document = await page.goto('/');
  expect(document?.headers()['x-frame-options']).toBe('DENY');
  expect(document?.headers()['content-security-policy']).toContain("frame-ancestors 'none'");
  const browserWrite = await page.evaluate(async () => {
    const response = await fetch('/api/v1/projects', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'PR10 same-origin browser' }),
    });
    return response.status;
  });
  expect(browserWrite).toBe(201);
  const listing = await request.get('/api/v1/projects');
  expect(listing.headers()['cache-control']).toBe('no-store');
  expect(listing.headers()['x-content-type-options']).toBe('nosniff');
  expect(listing.headers()['referrer-policy']).toBe('no-referrer');
  expect(listing.headers()['access-control-allow-origin']).toBeUndefined();
  const crossOrigin = await request.post('/api/v1/projects', {
    headers: { Origin: 'https://untrusted.example', 'Sec-Fetch-Site': 'cross-site' },
    data: { name: 'Rejected security fixture' },
  });
  expect(crossOrigin.status()).toBe(403);
  const oversized = await request.post('/api/v1/projects', { data: { name: 'x'.repeat(65_536) } });
  expect(oversized.status()).toBe(413);
  const malformed = await request.post('/api/v1/projects', {
    headers: { 'Content-Type': 'application/json' }, data: '{"name":"private-fixture-marker",',
  });
  expect(malformed.status()).toBe(400);
  expect(await malformed.text()).not.toMatch(/private-fixture-marker|stackTrace|SQLException|java\.lang/);
  expect((await request.get('/api/v1/projects?page=10001')).status()).toBe(400);
});

test('oversized reference is rejected and ordinary project deletion remains available', async ({ request }) => {
  const created = await request.post('/api/v1/projects', { data: { name: 'PR10 upload boundary' } });
  expect(created.status()).toBe(201);
  const project = await created.json();
  const bytes = Buffer.alloc(10 * 1024 * 1024 + 1);
  Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]).copy(bytes);
  const upload = await request.post(`/api/v1/projects/${project.id}/references`, {
    multipart: { title: 'Oversized fixture', guidance: '', file: { name: '../fixture.png', mimeType: 'image/png', buffer: bytes } },
  });
  expect(upload.status()).toBe(413);
  expect(await (await request.get(`/api/v1/projects/${project.id}/references`)).json()).toEqual([]);
  const deleted = await request.delete(`/api/v1/projects/${project.id}`, { data: { confirmationName: 'PR10 upload boundary' } });
  expect(deleted.status()).toBe(204);
  expect((await request.get(`/api/v1/projects/${project.id}`)).status()).toBe(404);
});
