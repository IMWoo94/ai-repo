import { expect, test } from '@playwright/test';

test('사용자가 로그인 후 지갑 조회, 충전, 송금, 운영 증거 확인을 수행한다', async ({ page }) => {
  await page.goto('/');

  // 로그인 전에는 wallet 액션 대신 로그인 폼이 보인다.
  await expect(page.getByRole('heading', { name: '회원 ID로 로그인해 내 지갑을 사용합니다.' })).toBeVisible();
  await page.getByLabel('회원 ID').fill('member-001');
  await page.getByRole('button', { name: '로그인' }).click();

  await expect(page.getByRole('heading', { name: '돈의 이동을 화면에서 바로 확인합니다.' })).toBeVisible();
  const balanceCard = page.locator('.balance-card');

  await expect(balanceCard.getByText('125,000 KRW')).toBeVisible();
  await expect(page.getByText('로그인되었습니다.')).toBeVisible();

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
  // 사용자 증거는 원장 + 소유자 스코프 감사 로그로 확인한다(step log/outbox는 운영자 전용).
  await expect(page.getByText(/op-\d+ · CREDIT · 잔액 132,000 KRW/)).toBeVisible();
  await expect(page.getByText(/op-\d+ · CHARGE_COMPLETED · /)).toBeVisible();

  await transferAmount.fill('3000');
  await page.getByRole('button', { name: '송금하기' }).click();

  await expect(page.getByText('송금이 완료되었습니다.')).toBeVisible();
  await expect(balanceCard.getByText('129,000 KRW')).toBeVisible();
  await expect(page.getByText(/최근 operation: op-\d+ · TRANSFER · COMPLETED/)).toBeVisible();
  // ADR-0058 수용된 노출: transfer 감사 detail에 양쪽 walletId가 보인다.
  await expect(page.getByText(/op-\d+ · TRANSFER_COMPLETED · Transfer completed from wallet-001 to wallet-002/)).toBeVisible();
});

test('잔액 부족 송금은 오류 메시지를 표시한다', async ({ page }) => {
  await page.goto('/');

  // wallet-002 소유자 member-002로 로그인하면 출금 지갑은 wallet-002로 파생된다.
  await page.getByLabel('회원 ID').fill('member-002');
  await page.getByRole('button', { name: '로그인' }).click();
  await expect(page.getByText('로그인되었습니다.')).toBeVisible();

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

test('운영자는 manual review event requeue를 요청, 승인, 실행하고 audit trail을 확인한다', async ({ page }) => {
  const fixtureResponse = await page.request.post('http://127.0.0.1:8080/api/v1/test-fixtures/outbox-events/manual-review', {
    headers: { 'X-Admin-Token': 'local-ops-token', 'X-Operator-Id': 'local-operator' },
  });
  expect(fixtureResponse.ok()).toBeTruthy();

  await page.goto('/');

  const operatorConsole = page.locator('.operator-console');
  await operatorConsole.getByRole('button', { name: 'Manual review 조회' }).click();

  await expect(operatorConsole.getByText('Manual review event 조회가 완료되었습니다.')).toBeVisible();
  await expect(operatorConsole.getByText('MANUAL_REVIEW').first()).toBeVisible();
  await expect(operatorConsole.getByText('e2e broker unavailable')).toBeVisible();

  await operatorConsole.getByLabel('Requeue 사유').fill('e2e broker recovered');
  await operatorConsole.getByRole('button', { name: 'Requeue 요청' }).click();

  await expect(operatorConsole.getByText('Requeue 요청이 등록되었습니다. 승인자를 분리해 승인하세요.')).toBeVisible();
  await expect(operatorConsole.getByText('REQUESTED')).toBeVisible();

  await operatorConsole.getByLabel('운영자 ID').fill('e2e-approver');
  await operatorConsole.getByLabel('Requeue 승인 사유').fill('operator verified broker recovery');
  await operatorConsole.getByRole('button', { name: 'Requeue 승인' }).click();

  await expect(operatorConsole.getByText('Requeue 요청이 승인되었습니다. 실행 단계로 진행하세요.')).toBeVisible();
  await expect(operatorConsole.locator('.status-badge').getByText('APPROVED', { exact: true })).toBeVisible();

  await operatorConsole.getByLabel('운영자 ID').fill('e2e-executor');
  await operatorConsole.getByRole('button', { name: 'Requeue 실행' }).click();

  await expect(operatorConsole.getByText('Requeue가 실행되었습니다. 감사 이력을 확인하세요.')).toBeVisible();
  await expect(operatorConsole.locator('.audit-trail strong').getByText('e2e-executor', { exact: true })).toBeVisible();
  await expect(operatorConsole.locator('.audit-trail').getByText('e2e broker recovered').last()).toBeVisible();
  await expect(operatorConsole.locator('.status-badge').getByText('EXECUTED', { exact: true })).toBeVisible();
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

test('운영자는 manual review requeue 요청을 반려하고 audit 없이 상태를 유지한다', async ({ page }) => {
  const fixtureResponse = await page.request.post('http://127.0.0.1:8080/api/v1/test-fixtures/outbox-events/manual-review', {
    headers: { 'X-Admin-Token': 'local-ops-token', 'X-Operator-Id': 'local-operator' },
  });
  expect(fixtureResponse.ok()).toBeTruthy();

  await page.goto('/');

  const operatorConsole = page.locator('.operator-console');
  await operatorConsole.getByRole('button', { name: 'Manual review 조회' }).click();
  await expect(operatorConsole.getByText('MANUAL_REVIEW').first()).toBeVisible();

  await operatorConsole.getByLabel('Requeue 사유').fill('e2e broker recovered');
  await operatorConsole.getByRole('button', { name: 'Requeue 요청' }).click();

  await expect(operatorConsole.getByText('REQUESTED')).toBeVisible();

  await operatorConsole.getByLabel('운영자 ID').fill('e2e-rejector');
  await operatorConsole.getByLabel('Requeue 반려 사유').fill('operator could not verify recovery');
  await operatorConsole.getByRole('button', { name: 'Requeue 반려' }).click();

  await expect(operatorConsole.getByText('Requeue 요청이 반려되었습니다. 감사 이력 없이 manual review 상태를 유지합니다.')).toBeVisible();
  await expect(operatorConsole.locator('.status-badge').getByText('REJECTED', { exact: true })).toBeVisible();
  await expect(operatorConsole.getByText('아직 requeue audit이 없습니다.')).toBeVisible();
  await expect(operatorConsole.getByText('MANUAL_REVIEW').first()).toBeVisible();
});
