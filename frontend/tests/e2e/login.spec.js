import { test, expect } from '@playwright/test';

test('login pobiera token i profil z API', async ({ page }) => {
  await page.goto('/login');

  await page.locator('#email').fill('user1@campus.edu.pl');
  await page.locator('#password').fill('Password123!');
  await page.getByRole('button', { name: /zaloguj/i }).click();

  await expect(page).toHaveURL(/\/equipment/, { timeout: 10_000 });

  const token = await page.evaluate(() => localStorage.getItem('token'));
  expect(token).toBeTruthy();

  await page.goto('/profile');
  await expect(page.getByText('user1@campus.edu.pl')).toBeVisible({ timeout: 10_000 });
});
