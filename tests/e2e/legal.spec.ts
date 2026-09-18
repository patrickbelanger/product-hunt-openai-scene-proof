import { expect, test } from '@playwright/test';

for (const width of [1280, 390]) {
  for (const [path, title] of [['/privacy', 'Privacy Policy'], ['/terms', 'Terms of Use']]) {
    test(`${path} direct navigation and reload at ${width}px make no API/provider calls`, async ({ page, context }) => {
      const apiRequests: string[] = [];
      const externalRequests: string[] = [];
      await page.route('**/api/**', route => { apiRequests.push(route.request().url()); return route.abort(); });
      await page.route('**/*', route => {
        if (new URL(route.request().url()).origin !== new URL(test.info().project.use.baseURL!).origin) {
          externalRequests.push(route.request().url());
          return route.abort();
        }
        return route.fallback();
      });
      await page.setViewportSize({ width, height: 900 });
      const response = await page.goto(path!);
      expect(response?.status()).toBe(200);
      await expect(page.getByRole('heading', { name: title, level: 1 })).toBeVisible();
      await expect(page).toHaveTitle(`${title} — SceneProof`);
      await page.reload();
      await expect(page.getByRole('heading', { name: title, level: 1 })).toBeVisible();
      const footer = page.getByRole('navigation', { name: 'Legal and source links' });
      await expect(footer.getByRole('link', { name: 'Privacy', exact: true })).toHaveAttribute('href', '/privacy');
      await expect(footer.getByRole('link', { name: 'Terms', exact: true })).toHaveAttribute('href', '/terms');
      await expect(footer.getByRole('link', { name: 'GitHub', exact: true })).toHaveAttribute('href', 'https://github.com/patrickbelanger/product-hunt-openai-scene-proof');
      await footer.getByRole('link', { name: path === '/privacy' ? 'Terms' : 'Privacy', exact: true }).click();
      await expect(page.getByRole('heading', { name: path === '/privacy' ? 'Terms of Use' : 'Privacy Policy', level: 1 })).toBeVisible();
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      expect(apiRequests).toEqual([]);
      expect(externalRequests).toEqual([]);
      expect(await context.cookies()).toEqual([]);
      expect(await page.evaluate(() => ({ local: localStorage.length, session: sessionStorage.length }))).toEqual({ local: 0, session: 0 });
      await page.screenshot({ path: `test-results/legal-${path!.slice(1)}-${width}.png`, fullPage: true });
    });
  }
}
