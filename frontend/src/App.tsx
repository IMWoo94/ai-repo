import { FormEvent, useEffect, useMemo, useState } from 'react';

type Money = {
  amount: number;
  currency: string;
};

type WalletBalance = {
  walletId: string;
  money: Money;
  asOf: string;
};

type TransactionHistoryItem = {
  transactionId: string;
  type: string;
  direction: string;
  money: Money;
  description: string;
  occurredAt: string;
};

type WalletOperationResult = {
  operationId: string;
  transactionId: string;
  walletId: string;
  counterpartyWalletId: string | null;
  occurredAt: string;
  type: string;
  status: string;
  direction: string;
  money: Money;
  balance: WalletBalance;
  description: string;
};

type LedgerEntry = {
  ledgerEntryId: string;
  operationId: string;
  walletId: string;
  type: string;
  direction: string;
  money: Money;
  balanceAfter: Money;
  description: string;
};

type AuditEvent = {
  auditEventId: string;
  operationId: string;
  type: string;
  detail: string;
  occurredAt: string;
};

type OperationStepLog = {
  operationStepLogId: string;
  operationId: string;
  step: string;
  status: string;
  detail: string;
};

type OperationOutboxEvent = {
  outboxEventId: string;
  operationId: string;
  eventType: string;
  status: string;
  attemptCount: number;
  lastError: string | null;
};

type ApiError = {
  code: string;
  message: string;
};

type ApiState = {
  balance?: WalletBalance;
  transactions: TransactionHistoryItem[];
  ledgerEntries: LedgerEntry[];
  auditEvents: AuditEvent[];
  stepLogs: OperationStepLog[];
  outboxEvents: OperationOutboxEvent[];
};

const initialApiState: ApiState = {
  transactions: [],
  ledgerEntries: [],
  auditEvents: [],
  stepLogs: [],
  outboxEvents: [],
};

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
    ...init,
  });

  if (!response.ok) {
    const error = (await response.json()) as ApiError;
    throw new Error(`${error.code}: ${error.message}`);
  }

  return response.json() as Promise<T>;
}

function formatMoney(money?: Money): string {
  if (!money) {
    return '-';
  }
  return `${money.amount.toLocaleString('ko-KR')} ${money.currency}`;
}

function uniqueKey(): string {
  return `ui-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function App() {
  const [walletId, setWalletId] = useState('wallet-001');
  const [targetWalletId, setTargetWalletId] = useState('wallet-002');
  const [amount, setAmount] = useState('5000');
  const [description, setDescription] = useState('프론트 테스트');
  const [operationId, setOperationId] = useState('');
  const [apiState, setApiState] = useState<ApiState>(initialApiState);
  const [lastOperation, setLastOperation] = useState<WalletOperationResult | null>(null);
  const [statusMessage, setStatusMessage] = useState('Spring Boot API를 실행한 뒤 데이터를 불러오세요.');
  const [isLoading, setIsLoading] = useState(false);

  const heroBalance = useMemo(() => formatMoney(apiState.balance?.money), [apiState.balance]);

  async function runAction(action: () => Promise<void>, successMessage: string) {
    setIsLoading(true);
    setStatusMessage('처리 중입니다.');
    try {
      await action();
      setStatusMessage(successMessage);
    } catch (error) {
      setStatusMessage(error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  }

  async function loadWalletEvidence(nextWalletId = walletId, nextOperationId = operationId) {
    const [balance, transactions, ledgerEntries, auditEvents] = await Promise.all([
      requestJson<WalletBalance>(`/api/v1/wallets/${nextWalletId}/balance`),
      requestJson<TransactionHistoryItem[]>(`/api/v1/wallets/${nextWalletId}/transactions`),
      requestJson<LedgerEntry[]>(`/api/v1/wallets/${nextWalletId}/ledger-entries`),
      requestJson<AuditEvent[]>('/api/v1/audit-events'),
    ]);

    const trimmedOperationId = nextOperationId.trim();
    const [stepLogs, outboxEvents] = trimmedOperationId
      ? await Promise.all([
        requestJson<OperationStepLog[]>(`/api/v1/operations/${trimmedOperationId}/step-logs`),
        requestJson<OperationOutboxEvent[]>(`/api/v1/operations/${trimmedOperationId}/outbox-events`),
      ])
      : [[], []];

    setApiState({ balance, transactions, ledgerEntries, auditEvents, stepLogs, outboxEvents });
  }

  async function submitCharge(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction(async () => {
      const result = await requestJson<WalletOperationResult>(`/api/v1/wallets/${walletId}/charges`, {
        method: 'POST',
        body: JSON.stringify({
          amount: Number(amount),
          currency: 'KRW',
          idempotencyKey: uniqueKey(),
          description,
        }),
      });
      setLastOperation(result);
      setOperationId(result.operationId);
      await loadWalletEvidence(walletId, result.operationId);
    }, '충전이 완료되었습니다.');
  }

  async function submitTransfer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction(async () => {
      const result = await requestJson<WalletOperationResult>(`/api/v1/wallets/${walletId}/transfers`, {
        method: 'POST',
        body: JSON.stringify({
          targetWalletId,
          amount: Number(amount),
          currency: 'KRW',
          idempotencyKey: uniqueKey(),
          description,
        }),
      });
      setLastOperation(result);
      setOperationId(result.operationId);
      await loadWalletEvidence(walletId, result.operationId);
    }, '송금이 완료되었습니다.');
  }

  useEffect(() => {
    void runAction(() => loadWalletEvidence('wallet-001', ''), '초기 데이터를 불러왔습니다.');
  }, []);

  return (
    <main>
      <nav className="global-nav">
        <span>AI Repo Fintech Lab</span>
        <span>Wallet · Ledger · Outbox</span>
      </nav>

      <section className="hero-tile">
        <div>
          <p className="eyebrow">Portfolio banking core</p>
          <h1>돈의 이동을 화면에서 바로 확인합니다.</h1>
          <p className="hero-copy">
            잔액, 거래내역, 원장, 감사 로그, Outbox까지 하나의 흐름으로 보여주는 React 사용자 화면입니다.
          </p>
          <div className="hero-actions">
            <button onClick={() => runAction(() => loadWalletEvidence(), '조회가 완료되었습니다.')} disabled={isLoading}>
              현재 지갑 조회
            </button>
            <a href="#evidence">증거 보기</a>
          </div>
        </div>
        <div className="balance-card">
          <span>현재 지갑</span>
          <strong>{walletId}</strong>
          <p>{heroBalance}</p>
          <small>{apiState.balance?.asOf ?? 'API 연결 대기'}</small>
        </div>
      </section>

      <section className="workspace-grid">
        <form className="panel form-panel" onSubmit={submitCharge}>
          <p className="eyebrow">Charge</p>
          <h2>충전</h2>
          <label>
            지갑 ID
            <input value={walletId} onChange={(event) => setWalletId(event.target.value)} />
          </label>
          <label>
            금액
            <input value={amount} type="number" min="1" onChange={(event) => setAmount(event.target.value)} />
          </label>
          <label>
            설명
            <input value={description} onChange={(event) => setDescription(event.target.value)} />
          </label>
          <button type="submit" disabled={isLoading}>충전하기</button>
        </form>

        <form className="panel form-panel dark-panel" onSubmit={submitTransfer}>
          <p className="eyebrow">Transfer</p>
          <h2>송금</h2>
          <label>
            출금 지갑
            <input value={walletId} onChange={(event) => setWalletId(event.target.value)} />
          </label>
          <label>
            입금 지갑
            <input value={targetWalletId} onChange={(event) => setTargetWalletId(event.target.value)} />
          </label>
          <label>
            금액
            <input value={amount} type="number" min="1" onChange={(event) => setAmount(event.target.value)} />
          </label>
          <button type="submit" disabled={isLoading}>송금하기</button>
        </form>
      </section>

      <section className="status-strip">
        <span>{isLoading ? '●' : '●'}</span>
        <p>{statusMessage}</p>
        <label>
          Operation ID
          <input value={operationId} onChange={(event) => setOperationId(event.target.value)} placeholder="충전/송금 후 자동 입력" />
        </label>
        {lastOperation && (
          <strong>
            최근 operation: {lastOperation.operationId} · {lastOperation.type} · {lastOperation.status}
          </strong>
        )}
      </section>

      <section id="evidence" className="evidence-layout">
        <EvidencePanel title="거래내역" items={apiState.transactions.map((item) => `${item.type} · ${item.direction} · ${formatMoney(item.money)} · ${item.description}`)} />
        <EvidencePanel title="원장" items={apiState.ledgerEntries.map((item) => `${item.operationId} · ${item.direction} · 잔액 ${formatMoney(item.balanceAfter)}`)} />
        <EvidencePanel title="감사 로그" items={apiState.auditEvents.map((item) => `${item.operationId} · ${item.type} · ${item.detail}`)} />
        <EvidencePanel title="Step Log" items={apiState.stepLogs.map((item) => `${item.step} · ${item.status} · ${item.detail}`)} />
        <EvidencePanel title="Outbox" items={apiState.outboxEvents.map((item) => `${item.outboxEventId} · ${item.eventType} · ${item.status} · attempt ${item.attemptCount}`)} />
      </section>
    </main>
  );
}

function EvidencePanel({ title, items }: { title: string; items: string[] }) {
  return (
    <article className="panel evidence-panel">
      <h3>{title}</h3>
      {items.length === 0 ? (
        <p className="empty">표시할 데이터가 없습니다.</p>
      ) : (
        <ul>
          {items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      )}
    </article>
  );
}
