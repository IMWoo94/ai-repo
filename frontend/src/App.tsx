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
  aggregateType: string;
  aggregateId: string;
  payload: string;
  status: string;
  occurredAt: string;
  attemptCount: number;
  nextRetryAt: string | null;
  claimedAt: string | null;
  leaseExpiresAt: string | null;
  publishedAt: string | null;
  lastError: string | null;
};

type OperationOutboxRequeueAudit = {
  auditId: string;
  outboxEventId: string;
  operationId: string;
  requeuedAt: string;
  operator: string;
  reason: string;
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

async function requestEmpty(url: string, init?: RequestInit): Promise<void> {
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
  const [chargeAmount, setChargeAmount] = useState('5000');
  const [transferAmount, setTransferAmount] = useState('5000');
  const [description, setDescription] = useState('프론트 테스트');
  const [operationId, setOperationId] = useState('');
  const [apiState, setApiState] = useState<ApiState>(initialApiState);
  const [lastOperation, setLastOperation] = useState<WalletOperationResult | null>(null);
  const [statusMessage, setStatusMessage] = useState('Spring Boot API를 실행한 뒤 데이터를 불러오세요.');
  const [isLoading, setIsLoading] = useState(false);
  const [adminToken, setAdminToken] = useState('local-ops-token');
  const [operatorId, setOperatorId] = useState('local-operator');
  const [manualReviewEvents, setManualReviewEvents] = useState<OperationOutboxEvent[]>([]);
  const [selectedOutboxEventId, setSelectedOutboxEventId] = useState('');
  const [requeueReason, setRequeueReason] = useState('broker recovered from operator console');
  const [requeueAudits, setRequeueAudits] = useState<OperationOutboxRequeueAudit[]>([]);
  const [operatorStatusMessage, setOperatorStatusMessage] = useState('운영자 token과 operator id를 입력한 뒤 manual review를 조회하세요.');
  const [isOperatorLoading, setIsOperatorLoading] = useState(false);

  const heroBalance = useMemo(() => formatMoney(apiState.balance?.money), [apiState.balance]);
  const selectedManualReviewEvent = useMemo(
    () => manualReviewEvents.find((event) => event.outboxEventId === selectedOutboxEventId) ?? null,
    [manualReviewEvents, selectedOutboxEventId],
  );

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

  async function runOperatorAction(action: () => Promise<void>, successMessage: string) {
    setIsOperatorLoading(true);
    setOperatorStatusMessage('운영 조치 처리 중입니다.');
    try {
      await action();
      setOperatorStatusMessage(successMessage);
    } catch (error) {
      setOperatorStatusMessage(error instanceof Error ? error.message : '운영 조치 중 알 수 없는 오류가 발생했습니다.');
    } finally {
      setIsOperatorLoading(false);
    }
  }

  function operatorHeaders(): HeadersInit {
    return {
      'X-Admin-Token': adminToken,
      'X-Operator-Id': operatorId,
    };
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

  async function loadManualReviewEvents() {
    const events = await requestJson<OperationOutboxEvent[]>('/api/v1/outbox-events/manual-review?limit=20', {
      headers: operatorHeaders(),
    });
    setManualReviewEvents(events);
    if (events.length === 0) {
      return;
    }
    if (!events.some((event) => event.outboxEventId === selectedOutboxEventId)) {
      setSelectedOutboxEventId(events[0].outboxEventId);
    }
  }

  async function loadRequeueAudits(outboxEventId = selectedOutboxEventId) {
    if (!outboxEventId) {
      setRequeueAudits([]);
      return;
    }
    const audits = await requestJson<OperationOutboxRequeueAudit[]>(
      `/api/v1/outbox-events/${outboxEventId}/requeue-audits`,
      { headers: operatorHeaders() },
    );
    setRequeueAudits(audits);
  }

  async function submitRequeue(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const outboxEventId = selectedOutboxEventId;
    await runOperatorAction(async () => {
      await requestEmpty(`/api/v1/outbox-events/${outboxEventId}/requeue`, {
        method: 'POST',
        headers: operatorHeaders(),
        body: JSON.stringify({ reason: requeueReason }),
      });
      await Promise.all([
        loadManualReviewEvents(),
        loadRequeueAudits(outboxEventId),
      ]);
    }, 'Requeue가 완료되었습니다. 감사 이력을 확인하세요.');
  }

  async function submitCharge(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction(async () => {
      const result = await requestJson<WalletOperationResult>(`/api/v1/wallets/${walletId}/charges`, {
        method: 'POST',
        body: JSON.stringify({
          amount: Number(chargeAmount),
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
          amount: Number(transferAmount),
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
            <input aria-label="충전 지갑 ID" value={walletId} onChange={(event) => setWalletId(event.target.value)} />
          </label>
          <label>
            금액
            <input aria-label="충전 금액" value={chargeAmount} type="number" min="1" onChange={(event) => setChargeAmount(event.target.value)} />
          </label>
          <label>
            설명
            <input aria-label="거래 설명" value={description} onChange={(event) => setDescription(event.target.value)} />
          </label>
          <button type="submit" disabled={isLoading}>충전하기</button>
        </form>

        <form className="panel form-panel dark-panel" onSubmit={submitTransfer}>
          <p className="eyebrow">Transfer</p>
          <h2>송금</h2>
          <label>
            출금 지갑
            <input aria-label="송금 출금 지갑 ID" value={walletId} onChange={(event) => setWalletId(event.target.value)} />
          </label>
          <label>
            입금 지갑
            <input aria-label="송금 입금 지갑 ID" value={targetWalletId} onChange={(event) => setTargetWalletId(event.target.value)} />
          </label>
          <label>
            금액
            <input aria-label="송금 금액" value={transferAmount} type="number" min="1" onChange={(event) => setTransferAmount(event.target.value)} />
          </label>
          <button type="submit" disabled={isLoading}>송금하기</button>
        </form>
      </section>

      <section className="status-strip">
        <span>{isLoading ? '●' : '●'}</span>
        <p>{statusMessage}</p>
        <label>
          Operation ID
          <input aria-label="Operation ID" value={operationId} onChange={(event) => setOperationId(event.target.value)} placeholder="충전/송금 후 자동 입력" />
        </label>
        {lastOperation && (
          <strong>
            최근 operation: {lastOperation.operationId} · {lastOperation.type} · {lastOperation.status}
          </strong>
        )}
      </section>

      <section className="operator-console" aria-labelledby="operator-console-title">
        <div className="section-heading">
          <p className="eyebrow">Operator console</p>
          <h2 id="operator-console-title">Manual review outbox를 운영자가 직접 확인합니다.</h2>
          <p>
            장애로 격리된 outbox event를 조회하고, 원인 조치 후 requeue하며, operator와 reason 감사 이력을 확인합니다.
          </p>
        </div>

        <div className="operator-grid">
          <article className="panel operator-card">
            <div className="card-heading">
              <p className="eyebrow">Access</p>
              <h3>운영자 header</h3>
            </div>
            <label>
              Admin token
              <input aria-label="운영자 Admin Token" value={adminToken} onChange={(event) => setAdminToken(event.target.value)} />
            </label>
            <label>
              Operator ID
              <input aria-label="운영자 ID" value={operatorId} onChange={(event) => setOperatorId(event.target.value)} />
            </label>
            <button
              type="button"
              onClick={() => runOperatorAction(loadManualReviewEvents, 'Manual review event 조회가 완료되었습니다.')}
              disabled={isOperatorLoading}
            >
              Manual review 조회
            </button>
            <StatusCallout message={operatorStatusMessage} tone={operatorStatusMessage.includes(':') ? 'error' : 'info'} />
          </article>

          <article className="panel operator-card operator-list-card">
            <div className="card-heading inline-heading">
              <div>
                <p className="eyebrow">Manual review</p>
                <h3>격리된 outbox</h3>
              </div>
              <span className="count-pill">{manualReviewEvents.length}</span>
            </div>
            {manualReviewEvents.length === 0 ? (
              <EmptyState
                title="Manual review 대기 event가 없습니다."
                description="조회 결과가 비어 있으면 운영 조치가 필요한 outbox가 없다는 뜻입니다."
              />
            ) : (
              <ul className="operator-event-list">
                {manualReviewEvents.map((event) => (
                  <li key={event.outboxEventId}>
                    <button
                      type="button"
                      className={event.outboxEventId === selectedOutboxEventId ? 'event-row selected' : 'event-row'}
                      onClick={() => {
                        setSelectedOutboxEventId(event.outboxEventId);
                        void runOperatorAction(() => loadRequeueAudits(event.outboxEventId), 'Requeue audit 조회가 완료되었습니다.');
                      }}
                    >
                      <span>
                        <strong>{event.outboxEventId}</strong>
                        <small>{event.operationId} · {event.eventType}</small>
                      </span>
                      <StatusBadge status={event.status} />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </article>

          <article className="panel operator-card requeue-card">
            <div className="card-heading">
              <p className="eyebrow">Requeue</p>
              <h3>재처리와 감사 이력</h3>
            </div>
            {!selectedOutboxEventId ? (
              <EmptyState
                title="선택된 outbox event가 없습니다."
                description="manual review event를 선택하면 requeue reason과 audit trail을 확인할 수 있습니다."
              />
            ) : (
              <>
                <div className="selected-event-card">
                  <StatusBadge status={selectedManualReviewEvent?.status ?? 'REQUEUED'} />
                  <strong>{selectedOutboxEventId}</strong>
                  <span>{selectedManualReviewEvent?.lastError ?? 'manual review 목록에서는 제외되었습니다.'}</span>
                  <small>
                    {selectedManualReviewEvent
                      ? `attempt ${selectedManualReviewEvent.attemptCount} · ${selectedManualReviewEvent.occurredAt}`
                      : '감사 이력으로 재처리 결과를 확인하세요.'}
                  </small>
                </div>
                <form className="requeue-form" onSubmit={submitRequeue}>
                  <label>
                    Requeue reason
                    <textarea
                      aria-label="Requeue 사유"
                      value={requeueReason}
                      onChange={(event) => setRequeueReason(event.target.value)}
                    />
                  </label>
                  <div className="operator-actions">
                    <button type="submit" disabled={isOperatorLoading || !selectedManualReviewEvent || requeueReason.trim().length === 0}>
                      Requeue 실행
                    </button>
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => runOperatorAction(() => loadRequeueAudits(), 'Requeue audit 조회가 완료되었습니다.')}
                      disabled={isOperatorLoading}
                    >
                      Audit 조회
                    </button>
                  </div>
                </form>
                <div className="audit-trail">
                  <h4>Requeue audit</h4>
                  {requeueAudits.length === 0 ? (
                    <p className="empty compact-empty">아직 requeue audit이 없습니다.</p>
                  ) : (
                    <ul>
                      {requeueAudits.map((audit) => (
                        <li key={audit.auditId}>
                          <strong>{audit.operator}</strong>
                          <span>{audit.reason}</span>
                          <small>{audit.requeuedAt}</small>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </>
            )}
          </article>
        </div>
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

function StatusBadge({ status }: { status: string }) {
  return <span className={`status-badge status-${status.toLowerCase().replaceAll('_', '-')}`}>{status}</span>;
}

function StatusCallout({ message, tone }: { message: string; tone: 'info' | 'error' }) {
  return (
    <p className={tone === 'error' ? 'status-callout error-callout' : 'status-callout'}>
      {message}
    </p>
  );
}

function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="empty-state">
      <strong>{title}</strong>
      <p>{description}</p>
    </div>
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
