import { test, expect } from '@playwright/test';

const OPIEKUN = { email: 'user2@campus.edu.pl', password: 'Password123!' };

async function opiekunToken(request) {
  const res = await request.post('/api/auth/login', { data: OPIEKUN });
  expect(res.ok()).toBeTruthy();
  const body = await res.json();
  return body.token;
}

// Sprzątanie po sobie: kasujemy sprzęt stworzony w teście (best-effort).
let opToken = null;
const createdIds = [];

test.afterEach(async ({ request }) => {
  if (!opToken) return;
  for (const id of createdIds.splice(0)) {
    await request.delete(`/api/equipment/${id}`, {
      headers: { Authorization: `Bearer ${opToken}` },
    });
  }
});

test('lista sprzętu pobiera dane z API i filtruje', async ({ page, request }) => {
  const token = await opiekunToken(request);
  opToken = token;
  const serial = `PW-${Date.now()}`;
  const deviceType = `PlaywrightRig ${serial}`;

  const createRes = await request.post('/api/equipment', {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      deviceType,
      technicalSpecification: 'e2e spec',
      serialNumber: serial,
      location: 'Lab e2e',
      status: 'DOSTEPNY',
    },
  });
  expect(createRes.status()).toBe(201);
  createdIds.push((await createRes.json()).id);

  await page.goto('/login');
  await page.locator('#email').fill(OPIEKUN.email);
  await page.locator('#password').fill(OPIEKUN.password);
  await page.getByRole('button', { name: /zaloguj/i }).click();
  await expect(page).toHaveURL(/\/equipment/, { timeout: 10_000 });

  await expect(page.getByText(deviceType).first()).toBeVisible({ timeout: 10_000 });
  await expect(page.getByText(serial).first()).toBeVisible();

  await page.getByPlaceholder(/szukaj/i).fill(serial);
  await expect(page.getByText(deviceType).first()).toBeVisible();
  await expect(page.getByText(serial).first()).toBeVisible();

  // Filtr lokalizacji idzie do backendu (LIKE, case-insensitive)
  await page.getByPlaceholder(/szukaj/i).fill('');
  await page.getByLabel(/lokalizacja/i).fill('studio nagrań');
  await expect(page.getByText('Interfejs Audio').first()).toBeVisible({ timeout: 10_000 });
  await expect(page.getByText(deviceType)).toHaveCount(0);
});
