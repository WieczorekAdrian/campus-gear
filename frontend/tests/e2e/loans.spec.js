import { test, expect } from '@playwright/test';

const OPIEKUN = { email: 'user2@campus.edu.pl', password: 'Password123!' };

function toLocalInputValue(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

let cleanupToken = null;
let cleanupEquipmentId = null;

test.afterEach(async ({ request }) => {
  if (!cleanupToken || !cleanupEquipmentId) return;
  await request.delete(`/api/equipment/${cleanupEquipmentId}`, {
    headers: { Authorization: `Bearer ${cleanupToken}` },
  });
  cleanupEquipmentId = null;
});

async function loginAs(page, email, password) {
  await page.goto('/login');
  await page.locator('#email').fill(email);
  await page.locator('#password').fill(password);
  await page.getByRole('button', { name: /zaloguj/i }).click();
  await expect(page).toHaveURL(/\/equipment/, { timeout: 10_000 });
}

async function logout(page) {
  await page.goto('/profile');
  await page.getByRole('button', { name: /wyloguj/i }).click();
  await expect(page).toHaveURL(/\/login/, { timeout: 10_000 });
}

test('podział ról: student rezerwuje, opiekun wydaje, student zwraca', async ({ page, request }) => {
  const stamp = Date.now();
  const student = {
    email: `pw.panel.${stamp}@campus.edu.pl`,
    password: 'Password123!',
    firstName: 'Pw',
    lastName: 'Panel',
  };

  const regRes = await request.post('/api/auth/register', { data: student });
  expect([200, 201]).toContain(regRes.status());

  const opLogin = await request.post('/api/auth/login', { data: OPIEKUN });
  expect(opLogin.ok()).toBeTruthy();
  const opToken = (await opLogin.json()).token;

  const serial = `PW-PANEL-${stamp}`;
  const deviceType = `PlaywrightPanel ${stamp}`;
  const eqRes = await request.post('/api/equipment', {
    headers: { Authorization: `Bearer ${opToken}` },
    data: {
      deviceType,
      technicalSpecification: 'e2e panel',
      serialNumber: serial,
      location: 'Lab e2e',
      status: 'DOSTEPNY',
    },
  });
  expect(eqRes.status()).toBe(201);
  const equipmentId = (await eqRes.json()).id;
  cleanupEquipmentId = equipmentId;
  cleanupToken = opToken;

  // 1. Student rezerwuje przez UI i NIE widzi akcji opiekuna
  await loginAs(page, student.email, student.password);
  await expect(page.getByRole('link', { name: /panel opiekuna/i })).toHaveCount(0);
  await page.getByPlaceholder(/szukaj/i).fill(serial);
  const eqRow = page.locator('tbody tr', { hasText: serial }).first();
  await expect(eqRow).toBeVisible({ timeout: 10_000 });
  await eqRow.getByRole('button', { name: /wypożycz/i }).click();
  await page.locator('input[type="datetime-local"]').nth(0)
    .fill(toLocalInputValue(new Date(Date.now() + 24 * 3600 * 1000)));
  await page.locator('input[type="datetime-local"]').nth(1)
    .fill(toLocalInputValue(new Date(Date.now() + 2 * 24 * 3600 * 1000)));
  await page.getByRole('button', { name: /potwierdź rezerwację/i }).click();
  await expect(page.getByText(/zarezerwowano pomyślnie/i)).toBeVisible({ timeout: 10_000 });

  await page.goto('/rentals');
  const resRow = page.locator('tbody tr', { hasText: serial }).first();
  await expect(resRow).toBeVisible({ timeout: 10_000 });
  await expect(resRow.getByRole('button', { name: /^wydaj$/i })).toHaveCount(0);

  // 2. Student nie wyda sobie sprzętu nawet przez API
  const stLogin = await request.post('/api/auth/login', {
    data: { email: student.email, password: student.password },
  });
  const stToken = (await stLogin.json()).token;
  const mineRes = await request.get('/api/reservations/mine', {
    headers: { Authorization: `Bearer ${stToken}` },
  });
  const reservationId = (await mineRes.json())[0].id;
  const forbiddenIssue = await request.post('/api/loans', {
    headers: { Authorization: `Bearer ${stToken}` },
    data: { reservationId },
  });
  expect(forbiddenIssue.status()).toBe(403);

  // 3. Opiekun wydaje w panelu
  await logout(page);
  await loginAs(page, OPIEKUN.email, OPIEKUN.password);
  await expect(page.getByRole('link', { name: /panel opiekuna/i })).toBeVisible();
  await page.goto('/panel');
  const panelRow = page.locator('tbody tr', { hasText: serial }).first();
  await expect(panelRow).toBeVisible({ timeout: 10_000 });
  await expect(panelRow.getByText(student.email)).toBeVisible();
  await panelRow.getByRole('button', { name: /^wydaj$/i }).click();
  await expect(page.getByText(/sprzęt wydany/i)).toBeVisible({ timeout: 10_000 });

  // Przed deklaracją zwrotu opiekun NIE ma guzika Odbierz
  await expect(page.locator('tbody tr', { hasText: serial }).filter({ hasText: /odbierz zwrot/i }))
    .toHaveCount(0);

  // 4. Student zgłasza zwrot (deklaracja, bez dialogu)
  await logout(page);
  await loginAs(page, student.email, student.password);
  await page.goto('/rentals');
  // Po wydaniu rezerwacja znika z aktywnych (status WYPOZYCZONA) - zostaje sam wiersz wypożyczenia
  const loanRow = page.locator('tbody tr', { hasText: serial }).first();
  await expect(loanRow).toBeVisible({ timeout: 10_000 });
  await loanRow.getByRole('button', { name: /^zwróć$/i }).click();
  await expect(page.getByText(/zgłoszony.*odbiór/i)).toBeVisible({ timeout: 10_000 });
  await expect(loanRow.getByText(/czeka na odbiór/i)).toBeVisible({ timeout: 10_000 });

  // 5. Opiekun odbiera zwrot z dialogiem uszkodzenia
  await logout(page);
  await loginAs(page, OPIEKUN.email, OPIEKUN.password);
  await page.goto('/panel');
  const panelLoanRow = page.locator('tbody tr', { hasText: serial }).first();
  await expect(panelLoanRow).toBeVisible({ timeout: 10_000 });
  await panelLoanRow.getByRole('button', { name: /odbierz zwrot/i }).click();
  await expect(page.getByText('Zwrot sprzętu')).toBeVisible({ timeout: 10_000 });
  await page.locator('input[type="checkbox"]').check();
  await page.locator('#damageDescription').fill('Pęknięta obudowa (e2e)');
  await page.getByRole('button', { name: /potwierdź zwrot/i }).click();
  await expect(page.getByText(/zgłoszony do serwisu/i)).toBeVisible({ timeout: 10_000 });

  const damagedCheck = await request.get('/api/equipment/search?status=SERWISOWANY', {
    headers: { Authorization: `Bearer ${stToken}` },
  });
  expect((await damagedCheck.json()).map((e) => e.serialNumber)).toContain(serial);

  // 6. Student widzi historię
  await logout(page);
  await loginAs(page, student.email, student.password);
  await page.goto('/rentals');
  await page.getByRole('button', { name: /^historia$/i }).click();
  await expect(page.locator('tbody tr', { hasText: serial }).last()).toBeVisible({ timeout: 10_000 });
});
