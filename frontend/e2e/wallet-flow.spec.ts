import { expect, test } from '@playwright/test';

test('사용자가 지갑 조회, 충전, 송금, 운영 증거 확인을 수행한다', async ({ page }) => {
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

  await transferAmount.fill('3000');
  await page.getByRole('button', { name: '송금하기' }).click();

  await expect(page.getByText('송금이 완료되었습니다.')).toBeVisible();
  await expect(balanceCard.getByText('129,000 KRW')).toBeVisible();
  await expect(page.getByText(/최근 operation: op-\d+ · TRANSFER · COMPLETED/)).toBeVisible();
  await expect(page.getByText(/TRANSFER_COMPLETED · PENDING/)).toBeVisible();
});

test('잔액 부족 송금은 오류 메시지를 표시한다', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByText('초기 데이터를 불러왔습니다.')).toBeVisible();

  await page.getByLabel('송금 출금 지갑 ID').fill('wallet-002');
  await page.getByLabel('송금 입금 지갑 ID').fill('wallet-001');
  await page.getByLabel('송금 금액').fill('999999');
  await page.getByRole('button', { name: '송금하기' }).click();

  await expect(page.getByText('INSUFFICIENT_BALANCE: Insufficient balance: wallet-002')).toBeVisible();
});

test('운영자는 manual review 콘솔의 인증 오류와 empty state를 확인한다', async ({ page }) => {
  await page.goto('/');

  const operatorConsole = page.locator('.operator-console');

  await expect(operatorConsole.getByRole('heading', { name: 'Manual review outbox를 운영자가 직접 확인합니다.' })).toBeVisible();
  await expect(operatorConsole.getByLabel('운영자 Admin Token')).toHaveValue('local-ops-token');
  await expect(operatorConsole.getByLabel('운영자 Operator Token')).toHaveValue('local-operator-token');
  await expect(operatorConsole.getByLabel('운영자 ID')).toHaveValue('local-operator');
  await expect(operatorConsole.getByText('Manual review 대기 event가 없습니다.')).toBeVisible();
  await expect(operatorConsole.getByText('선택된 outbox event가 없습니다.')).toBeVisible();

  await operatorConsole.getByLabel('운영자 Admin Token').fill('wrong-token');
  await operatorConsole.getByLabel('운영자 Operator Token').fill('wrong-token');
  await operatorConsole.getByRole('button', { name: 'Manual review 조회' }).click();

  await expect(operatorConsole.getByText(/ADMIN_AUTHENTICATION_REQUIRED/)).toBeVisible();

  await operatorConsole.getByLabel('운영자 Admin Token').fill('local-ops-token');
  await operatorConsole.getByLabel('운영자 Operator Token').fill('local-operator-token');
  await operatorConsole.getByRole('button', { name: 'Manual review 조회' }).click();

  await expect(operatorConsole.getByText('Manual review event 조회가 완료되었습니다.')).toBeVisible();
  await expect(operatorConsole.getByText('Manual review 대기 event가 없습니다.')).toBeVisible();
});

test('운영자는 manual review event를 requeue하고 audit trail을 확인한다', async ({ page }) => {
  const fixtureResponse = await page.request.post('http://127.0.0.1:8080/api/v1/test-fixtures/outbox-events/manual-review');
  expect(fixtureResponse.ok()).toBeTruthy();

  await page.goto('/');

  const operatorConsole = page.locator('.operator-console');
  await operatorConsole.getByRole('button', { name: 'Manual review 조회' }).click();

  await expect(operatorConsole.getByText('Manual review event 조회가 완료되었습니다.')).toBeVisible();
  await expect(operatorConsole.getByText('MANUAL_REVIEW').first()).toBeVisible();
  await expect(operatorConsole.getByText('e2e broker unavailable')).toBeVisible();

  await operatorConsole.getByLabel('Requeue 사유').fill('e2e broker recovered');
  await operatorConsole.getByRole('button', { name: 'Requeue 실행' }).click();

  await expect(operatorConsole.getByText('Requeue가 완료되었습니다. 감사 이력을 확인하세요.')).toBeVisible();
  await expect(operatorConsole.getByText('local-operator')).toBeVisible();
  await expect(operatorConsole.locator('.audit-trail').getByText('e2e broker recovered')).toBeVisible();
  await expect(operatorConsole.getByText('REQUEUED')).toBeVisible();
});

test('운영자는 relay health와 pruning 결과를 화면에서 확인한다', async ({ page }) => {
  await page.goto('/');

  const operatorConsole = page.locator('.operator-console');
  await operatorConsole.getByRole('button', { name: 'Relay 상태 조회' }).click();

  await expect(operatorConsole.getByText('Relay health와 실행 기록 조회가 완료되었습니다.')).toBeVisible();
  await expect(operatorConsole.getByText('Scheduler 상태')).toBeVisible();

  await operatorConsole.getByRole('button', { name: 'Pruning 실행' }).click();

  await expect(operatorConsole.getByText('운영 로그 pruning이 완료되었습니다.')).toBeVisible();
  await expect(operatorConsole.getByText('Relay run 삭제')).toBeVisible();
  await expect(operatorConsole.getByText('Access audit 삭제')).toBeVisible();
});
