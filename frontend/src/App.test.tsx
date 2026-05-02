import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { App } from './App';

type MockFetchState = {
  balanceAmount: number;
  operationId: string | null;
  operationType: 'CHARGE' | 'TRANSFER' | null;
};

function setupFetch(state: MockFetchState = { balanceAmount: 125000, operationId: null, operationType: null }) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = input.toString();
    const method = init?.method ?? 'GET';

    if (url.endsWith('/balance')) {
      return jsonResponse({
        walletId: 'wallet-001',
        money: { amount: state.balanceAmount, currency: 'KRW' },
        asOf: '2026-05-02T00:00:00Z',
      });
    }

    if (url.endsWith('/transactions')) {
      return jsonResponse([]);
    }

    if (url.endsWith('/ledger-entries')) {
      return jsonResponse(state.operationId ? [
        {
          ledgerEntryId: 'ledger-001',
          operationId: state.operationId,
          walletId: 'wallet-001',
          type: state.operationType,
          direction: state.operationType === 'TRANSFER' ? 'DEBIT' : 'CREDIT',
          money: { amount: 7000, currency: 'KRW' },
          balanceAfter: { amount: state.balanceAmount, currency: 'KRW' },
          description: '프론트 테스트',
        },
      ] : []);
    }

    if (url.endsWith('/audit-events')) {
      return jsonResponse([]);
    }

    if (url.endsWith('/step-logs')) {
      return jsonResponse(state.operationId ? [
        {
          operationStepLogId: 'step-001',
          operationId: state.operationId,
          step: 'LEDGER_RECORDED',
          status: 'COMPLETED',
          detail: 'Ledger entry recorded for wallet wallet-001',
        },
      ] : []);
    }

    if (url.endsWith('/outbox-events')) {
      return jsonResponse(state.operationId ? [
        {
          outboxEventId: 'outbox-001',
          operationId: state.operationId,
          eventType: `${state.operationType}_COMPLETED`,
          status: 'PENDING',
          attemptCount: 0,
          lastError: null,
        },
      ] : []);
    }

    if (url.endsWith('/charges') && method === 'POST') {
      const body = JSON.parse(String(init?.body));
      state.balanceAmount = 125000 + Number(body.amount);
      state.operationId = 'op-001';
      state.operationType = 'CHARGE';
      return jsonResponse(operationResult({
        operationId: 'op-001',
        transactionId: 'txn-001',
        type: 'CHARGE',
        direction: 'CREDIT',
        amount: Number(body.amount),
        balanceAmount: state.balanceAmount,
      }), 201);
    }

    if (url.endsWith('/transfers') && method === 'POST') {
      const body = JSON.parse(String(init?.body));
      if (body.targetWalletId === 'wallet-001' && Number(body.amount) > 30000) {
        return jsonResponse({
          code: 'INSUFFICIENT_BALANCE',
          message: 'Insufficient balance: wallet-002',
          timestamp: '2026-05-02T00:00:00Z',
        }, 409);
      }
      state.balanceAmount = 125000 - Number(body.amount);
      state.operationId = 'op-001';
      state.operationType = 'TRANSFER';
      return jsonResponse(operationResult({
        operationId: 'op-001',
        transactionId: 'txn-001',
        type: 'TRANSFER',
        direction: 'DEBIT',
        amount: Number(body.amount),
        balanceAmount: state.balanceAmount,
        counterpartyWalletId: body.targetWalletId,
      }), 201);
    }

    throw new Error(`Unhandled request: ${method} ${url}`);
  });

  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

function jsonResponse(body: unknown, status = 200) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as Response;
}

function operationResult({
  operationId,
  transactionId,
  type,
  direction,
  amount,
  balanceAmount,
  counterpartyWalletId = null,
}: {
  operationId: string;
  transactionId: string;
  type: string;
  direction: string;
  amount: number;
  balanceAmount: number;
  counterpartyWalletId?: string | null;
}) {
  return {
    operationId,
    transactionId,
    walletId: 'wallet-001',
    counterpartyWalletId,
    occurredAt: '2026-05-02T00:00:00Z',
    type,
    status: 'COMPLETED',
    direction,
    money: { amount, currency: 'KRW' },
    balance: {
      walletId: 'wallet-001',
      money: { amount: balanceAmount, currency: 'KRW' },
      asOf: '2026-05-02T00:00:00Z',
    },
    description: '프론트 테스트',
  };
}

describe('App', () => {
  it('초기 지갑 잔액을 렌더링한다', async () => {
    setupFetch();

    render(<App />);

    expect(await screen.findByText('125,000 KRW')).toBeVisible();
    expect(screen.getByText('초기 데이터를 불러왔습니다.')).toBeVisible();
  });

  it('충전 금액과 송금 금액 입력은 독립적으로 동작한다', async () => {
    const user = userEvent.setup();
    setupFetch();

    render(<App />);

    await screen.findByText('125,000 KRW');
    const chargeAmount = screen.getByLabelText('충전 금액');
    const transferAmount = screen.getByLabelText('송금 금액');

    await user.clear(chargeAmount);
    await user.type(chargeAmount, '7000');

    expect(transferAmount).toHaveValue(5000);

    await user.clear(transferAmount);
    await user.type(transferAmount, '3000');

    expect(chargeAmount).toHaveValue(7000);
  });

  it('충전 요청 payload와 operation 증거를 갱신한다', async () => {
    const user = userEvent.setup();
    const fetchMock = setupFetch();

    render(<App />);

    await screen.findByText('125,000 KRW');
    await user.clear(screen.getByLabelText('충전 금액'));
    await user.type(screen.getByLabelText('충전 금액'), '7000');
    await user.click(screen.getByRole('button', { name: '충전하기' }));

    await screen.findByText('충전이 완료되었습니다.');
    expect(screen.getByText('132,000 KRW')).toBeVisible();
    expect(screen.getByText(/최근 operation: op-001 · CHARGE · COMPLETED/)).toBeVisible();
    expect(screen.getByText(/CHARGE_COMPLETED · PENDING/)).toBeVisible();

    const chargeCall = fetchMock.mock.calls.find(([url]) => url.toString().endsWith('/charges'));
    expect(chargeCall).toBeDefined();
    expect(JSON.parse(String(chargeCall?.[1]?.body))).toMatchObject({
      amount: 7000,
      currency: 'KRW',
      description: '프론트 테스트',
    });
  });

  it('잔액 부족 송금 오류를 표시한다', async () => {
    const user = userEvent.setup();
    setupFetch();

    render(<App />);

    await screen.findByText('125,000 KRW');
    await user.clear(screen.getByLabelText('송금 출금 지갑 ID'));
    await user.type(screen.getByLabelText('송금 출금 지갑 ID'), 'wallet-002');
    await user.clear(screen.getByLabelText('송금 입금 지갑 ID'));
    await user.type(screen.getByLabelText('송금 입금 지갑 ID'), 'wallet-001');
    await user.clear(screen.getByLabelText('송금 금액'));
    await user.type(screen.getByLabelText('송금 금액'), '999999');
    await user.click(screen.getByRole('button', { name: '송금하기' }));

    await waitFor(() => {
      expect(screen.getByText('INSUFFICIENT_BALANCE: Insufficient balance: wallet-002')).toBeVisible();
    });
  });
});
