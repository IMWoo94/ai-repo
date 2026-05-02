import { expect, test } from '@playwright/test';

test('사용자가 지갑 조회, 충전, 운영 증거 확인을 수행한다', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByRole('heading', { name: '돈의 이동을 화면에서 바로 확인합니다.' })).toBeVisible();
  const balanceCard = page.locator('.balance-card');

  await expect(balanceCard.getByText('125,000 KRW')).toBeVisible();
  await expect(page.getByText('초기 데이터를 불러왔습니다.')).toBeVisible();

  const chargeAmount = page.getByLabel('충전 금액');
  const transferAmount = page.getByLabel('송금 금액');

  await chargeAmount.fill('7000');
  await expect(transferAmount).toHaveValue('5000');

  await transferAmount.fill('3000');
  await expect(chargeAmount).toHaveValue('7000');

  await page.getByRole('button', { name: '충전하기' }).click();

  await expect(page.getByText('충전이 완료되었습니다.')).toBeVisible();
  await expect(balanceCard.getByText('132,000 KRW')).toBeVisible();
  await expect(page.getByText(/최근 operation: op-\d+ · CHARGE · COMPLETED/)).toBeVisible();
  await expect(page.getByText(/LEDGER_RECORDED · COMPLETED/)).toBeVisible();
  await expect(page.getByText(/CHARGE_COMPLETED · PENDING/)).toBeVisible();
});
