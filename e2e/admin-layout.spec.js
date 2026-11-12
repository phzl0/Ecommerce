import { test, expect } from '@playwright/test';

test('administrative screens keep controls usable on desktop and mobile', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('E-mail', { exact: true }).fill('admin@level.local');
  await page.getByLabel('Senha', { exact: true }).fill('AdminLocal123!');
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect(page).not.toHaveURL(/\/login/);
  for (const route of ['/admin', '/admin/produtos', '/admin/categorias', '/admin/pedidos', '/admin/fidelidade', '/admin/eventos', '/admin/integracoes', '/admin/estoque', '/admin/digitais']) {
    await page.goto(route);
    await expect(page.locator('main h1')).toBeVisible();
    await expect(page.locator('.sidebar-nav [aria-current="page"]')).toHaveCount(1);
    await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  }
  await page.goto('/admin/fidelidade');
  await page.getByRole('link', { name: 'Missões', exact: true }).click();
  await expect(page).toHaveURL(/#missoes$/);
  await expect(page.locator('#missoes')).toBeInViewport();
  await page.screenshot({ path: 'test-results/admin-desktop.png', fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  for (const route of ['/admin', '/admin/produtos', '/admin/categorias', '/admin/pedidos', '/admin/fidelidade', '/admin/eventos', '/admin/integracoes', '/admin/estoque', '/admin/digitais']) {
    await page.goto(route);
    await expect(page.locator('.sidebar-nav')).not.toBeVisible();
    expect((await page.locator('.admin-sidebar').boundingBox()).height).toBeLessThan(160);
    await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  }
  await page.locator('.admin-navigation > summary').click();
  await expect(page.getByRole('link', { name: 'Passe, VIP e Baú', exact: true })).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(page.locator('.sidebar-nav')).not.toBeVisible();
  await page.screenshot({ path: 'test-results/admin-mobile.png', fullPage: true });
});
