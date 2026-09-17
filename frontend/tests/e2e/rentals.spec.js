import { test, expect } from '@playwright/test';

const OPIEKUN = { email: 'user2@campus.edu.pl', password: 'Password123!' };

function toLocalInputValue(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

// Sprzątanie po sobie: kasujemy sprzęt stworzony w teście (best-effort,
// rezerwacja schodzi razem z nim przez cascade).
let cleanupToken = null;
let cleanupEquipmentId = null;

test.afterEach(async ({ request }) => {
  if (!cleanupToken || !cleanupEquipmentId) return;
  await request.delete(`/api/equipment/${cleanupEquipmentId}`, {
    headers: { Authorization: `Bearer ${cleanupToken}` },
  });
  cleanupEquipmentId = null;
});

test('rezerwacja z UI i anulowanie z /rentals', async ({ page, request }) => {
  const stamp = Date.now();
  const student = {
    email: `pw.student.${stamp}@campus.edu.pl`,
    password: 'Password123!',
    firstName: 'Pw',
    lastName: 'Student',
  };

  const regRes = await request.post('/api/auth/register', { data: student });
  expect([200, 201]).toContain(regRes.status());

  const opLogin = await request.post('/api/auth/login', { data: OPIEKUN });
  expect(opLogin.ok()).toBeTruthy();
  const opToken = (await opLogin.json()).token;

  const serial = `PW-RENT-${stamp}`;
  const deviceType = `PlaywrightRent ${stamp}`;
  const eqRes = await request.post('/api/equipment', {
    headers: { Authorization: `Bearer ${opToken}` },
    data: {
      deviceType,
      technicalSpecification: 'e2e rent',
      serialNumber: serial,
      location: 'Lab e2e',
      status: 'DOSTEPNY',
    },
  });
  expect(eqRes.status()).toBe(201);
  cleanupEquipmentId = (await eqRes.json()).id;
  cleanupToken = opToken;

  await page.goto('/login');
  await page.locator('#email').fill(student.email);
  await page.locator('#password').fill(student.password);
  await page.getByRole('button', { name: /zaloguj/i }).click();
  await expect(page).toHaveURL(/\/equipment/, { timeout: 10_000 });

  await page.getByPlaceholder(/szukaj/i).fill(serial);
  const row = page.locator('tbody tr', { hasText: serial }).first();
  await expect(row).toBeVisible({ timeout: 10_000 });
  await row.getByRole('button', { name: /wypożycz/i }).click();

  const start = toLocalInputValue(new Date(Date.now() + 24 * 3600 * 1000));
  const end = toLocalInputValue(new Date(Date.now() + 2 * 24 * 3600 * 1000));
  await page.locator('input[type="datetime-local"]').nth(0).fill(start);
  await page.locator('input[type="datetime-local"]').nth(1).fill(end);
  await page.getByRole('button', { name: /potwierdź rezerwację/i }).click();
  await expect(page.getByText(/zarezerwowano pomyślnie/i)).toBeVisible({ timeout: 10_000 });

  await page.goto('/rentals');
  const rentRow = page.locator('tbody tr', { hasText: serial }).first();
  await expect(rentRow).toBeVisible({ timeout: 10_000 });
  await rentRow.getByRole('button', { name: /anuluj/i }).click();

  await expect(page.locator('tbody', { hasText: serial })).toHaveCount(0, { timeout: 10_000 });

  await page.getByRole('button', { name: /^historia$/i }).click();
  await expect(page.locator('tbody tr', { hasText: serial }).first()).toBeVisible({ timeout: 10_000 });
  await expect(page.locator('tbody tr', { hasText: serial }).first()).toContainText('ANULOWANA');
});
