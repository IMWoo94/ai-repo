package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.OperationOutboxRelayRepository;
import com.imwoo.airepo.wallet.application.InvalidWalletOperationException;
import com.imwoo.airepo.wallet.application.WalletCommandRepository;
import com.imwoo.airepo.wallet.application.WalletLedgerQueryRepository;
import com.imwoo.airepo.wallet.application.WalletOperationRecord;
import com.imwoo.airepo.wallet.application.WalletOperationResult;
import com.imwoo.airepo.wallet.domain.AuditEvent;
import com.imwoo.airepo.wallet.domain.AuditEventType;
import com.imwoo.airepo.wallet.domain.LedgerEntry;
import com.imwoo.airepo.wallet.domain.Member;
import com.imwoo.airepo.wallet.domain.MemberStatus;
import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueAudit;
import com.imwoo.airepo.wallet.domain.OperationOutboxStatus;
import com.imwoo.airepo.wallet.domain.OperationStep;
import com.imwoo.airepo.wallet.domain.OperationStepLog;
import com.imwoo.airepo.wallet.domain.TransactionDirection;
import com.imwoo.airepo.wallet.domain.TransactionHistoryItem;
import com.imwoo.airepo.wallet.domain.TransactionStatus;
import com.imwoo.airepo.wallet.domain.TransactionType;
import com.imwoo.airepo.wallet.domain.WalletAccount;
import com.imwoo.airepo.wallet.domain.WalletAccountStatus;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class InMemoryWalletRepository implements
        WalletCommandRepository,
        WalletLedgerQueryRepository,
        OperationOutboxRelayRepository {

    private static final String DEFAULT_CURRENCY = "KRW";

    private final Map<String, Member> members = new HashMap<>();
    private final Map<String, WalletAccount> walletAccounts = new HashMap<>();
    private final Map<String, WalletBalance> balances = new HashMap<>();
    private final Map<String, List<TransactionHistoryItem>> transactions = new HashMap<>();
    private final Map<String, List<LedgerEntry>> ledgerEntries = new HashMap<>();
    private final List<AuditEvent> auditEvents = new ArrayList<>();
    private final Map<String, List<OperationStepLog>> operationStepLogs = new HashMap<>();
    private final Map<String, List<OperationOutboxEvent>> operationOutboxEvents = new HashMap<>();
    private final Map<String, List<OperationOutboxRequeueAudit>> outboxRequeueAudits = new HashMap<>();
    private final Map<String, WalletOperationRecord> operations = new HashMap<>();
    private int transactionSequence = 2;
    private int operationSequence = 0;
    private int ledgerEntrySequence = 0;
    private int auditEventSequence = 0;
    private int operationStepLogSequence = 0;
    private int outboxEventSequence = 0;
    private int outboxRequeueAuditSequence = 0;

    public InMemoryWalletRepository() {
        members.put(
                "member-001",
                new Member("member-001", MemberStatus.ACTIVE, Instant.parse("2026-05-01T00:00:00Z"))
        );
        members.put(
                "member-002",
                new Member("member-002", MemberStatus.ACTIVE, Instant.parse("2026-05-01T00:00:00Z"))
        );
        walletAccounts.put(
                "wallet-001",
                new WalletAccount(
                        "wallet-001",
                        "member-001",
                        WalletAccountStatus.ACTIVE,
                        Instant.parse("2026-05-01T00:00:00Z")
                )
        );
        walletAccounts.put(
                "wallet-002",
                new WalletAccount(
                        "wallet-002",
                        "member-002",
                        WalletAccountStatus.ACTIVE,
                        Instant.parse("2026-05-01T00:00:00Z")
                )
        );
        balances.put(
                "wallet-001",
                new WalletBalance(
                        "wallet-001",
                        new Money(new BigDecimal("125000"), DEFAULT_CURRENCY),
                        Instant.parse("2026-05-01T00:00:00Z")
                )
        );
        balances.put(
                "wallet-002",
                new WalletBalance(
                        "wallet-002",
                        new Money(new BigDecimal("30000"), DEFAULT_CURRENCY),
                        Instant.parse("2026-05-01T00:00:00Z")
                )
        );
        transactions.put(
                "wallet-001",
                new ArrayList<>(List.of(
                        new TransactionHistoryItem(
                                "txn-002",
                                "wallet-001",
                                Instant.parse("2026-05-01T00:00:00Z"),
                                TransactionType.REWARD,
                                TransactionStatus.COMPLETED,
                                TransactionDirection.CREDIT,
                                new Money(new BigDecimal("25000"), DEFAULT_CURRENCY),
                                "학습용 리워드 적립"
                        ),
                        new TransactionHistoryItem(
                                "txn-001",
                                "wallet-001",
                                Instant.parse("2026-04-30T00:00:00Z"),
                                TransactionType.CHARGE,
                                TransactionStatus.COMPLETED,
                                TransactionDirection.CREDIT,
                                new Money(new BigDecimal("100000"), DEFAULT_CURRENCY),
                                "학습용 충전"
                        )
                ))
        );
        transactions.put("wallet-002", new ArrayList<>());
        ledgerEntries.put("wallet-001", new ArrayList<>());
        ledgerEntries.put("wallet-002", new ArrayList<>());
    }

    @Override
    public synchronized Optional<Member> findMember(String memberId) {
        return Optional.ofNullable(members.get(memberId));
    }

    @Override
    public synchronized Optional<WalletAccount> findWalletAccount(String walletId) {
        return Optional.ofNullable(walletAccounts.get(walletId));
    }

    @Override
    public synchronized Optional<WalletBalance> findBalance(String walletId) {
        return Optional.ofNullable(balances.get(walletId));
    }

    @Override
    public synchronized List<TransactionHistoryItem> findTransactions(String walletId) {
        return List.copyOf(transactions.getOrDefault(walletId, List.of()));
    }

    @Override
    public synchronized List<LedgerEntry> findLedgerEntries(String walletId) {
        return List.copyOf(ledgerEntries.getOrDefault(walletId, List.of()));
    }

    @Override
    public synchronized List<AuditEvent> findAuditEvents() {
        return List.copyOf(auditEvents);
    }

    @Override
    public synchronized List<OperationStepLog> findOperationStepLogs(String operationId) {
        return List.copyOf(operationStepLogs.getOrDefault(operationId, List.of()));
    }

    @Override
    public synchronized List<OperationOutboxEvent> findOperationOutboxEvents(String operationId) {
        return List.copyOf(operationOutboxEvents.getOrDefault(operationId, List.of()));
    }

    @Override
    public synchronized List<OperationOutboxEvent> findPendingOutboxEvents(int limit) {
        return operationOutboxEvents.values().stream()
                .flatMap(List::stream)
                .filter(outboxEvent -> outboxEvent.status() == OperationOutboxStatus.PENDING)
                .sorted(this::compareOutboxEvents)
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized List<OperationOutboxEvent> findManualReviewOutboxEvents(int limit) {
        return operationOutboxEvents.values().stream()
                .flatMap(List::stream)
                .filter(outboxEvent -> outboxEvent.status() == OperationOutboxStatus.MANUAL_REVIEW)
                .sorted(this::compareOutboxEvents)
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized List<OperationOutboxRequeueAudit> findOutboxRequeueAudits(String outboxEventId) {
        return List.copyOf(outboxRequeueAudits.getOrDefault(outboxEventId, List.of()));
    }

    @Override
    public synchronized List<OperationOutboxEvent> claimReadyOutboxEvents(int limit, Instant now, Instant leaseExpiresAt) {
        List<OperationOutboxEvent> claimedEvents = operationOutboxEvents.values().stream()
                .flatMap(List::stream)
                .filter(outboxEvent -> isReadyToClaim(outboxEvent, now))
                .sorted(this::compareOutboxEvents)
                .limit(limit)
                .map(outboxEvent -> processingOutboxEvent(outboxEvent, now, leaseExpiresAt))
                .toList();
        for (OperationOutboxEvent claimedEvent : claimedEvents) {
            replaceOutboxEvent(claimedEvent.outboxEventId(), ignored -> claimedEvent);
        }
        return claimedEvents;
    }

    @Override
    public synchronized void markOutboxEventPublished(String outboxEventId, Instant publishedAt) {
        replaceOutboxEvent(outboxEventId, event -> new OperationOutboxEvent(
                event.outboxEventId(),
                event.operationId(),
                event.eventType(),
                event.aggregateType(),
                event.aggregateId(),
                event.payload(),
                OperationOutboxStatus.PUBLISHED,
                event.occurredAt(),
                event.attemptCount(),
                null,
                null,
                null,
                publishedAt,
                null
        ));
    }

    @Override
    public synchronized void markOutboxEventFailed(
            String outboxEventId,
            String lastError,
            Instant nextRetryAt,
            int maxAttempts
    ) {
        replaceOutboxEvent(outboxEventId, event -> new OperationOutboxEvent(
                event.outboxEventId(),
                event.operationId(),
                event.eventType(),
                event.aggregateType(),
                event.aggregateId(),
                event.payload(),
                failedStatus(event, maxAttempts),
                event.occurredAt(),
                event.attemptCount() + 1,
                failedNextRetryAt(event, nextRetryAt, maxAttempts),
                null,
                null,
                null,
                lastError
        ));
    }

    @Override
    public synchronized void requeueManualReviewOutboxEvent(
            String outboxEventId,
            Instant requeuedAt,
            String operator,
            String reason
    ) {
        OperationOutboxEvent event = findOutboxEvent(outboxEventId);
        if (event.status() != OperationOutboxStatus.MANUAL_REVIEW) {
            throw new InvalidWalletOperationException("outboxEventId must be in MANUAL_REVIEW: " + outboxEventId);
        }
        replaceOutboxEvent(outboxEventId, ignored -> new OperationOutboxEvent(
                event.outboxEventId(),
                event.operationId(),
                event.eventType(),
                event.aggregateType(),
                event.aggregateId(),
                event.payload(),
                OperationOutboxStatus.PENDING,
                event.occurredAt(),
                0,
                null,
                null,
                null,
                null,
                null
        ));
        outboxRequeueAudits.computeIfAbsent(outboxEventId, ignored -> new ArrayList<>())
                .add(new OperationOutboxRequeueAudit(
                        nextOutboxRequeueAuditId(),
                        outboxEventId,
                        event.operationId(),
                        requeuedAt,
                        operator,
                        reason
                ));
    }

    @Override
    public synchronized Optional<WalletOperationRecord> findOperation(String idempotencyKey) {
        return Optional.ofNullable(operations.get(idempotencyKey));
    }

    @Override
    public synchronized WalletOperationRecord applyCharge(
            String idempotencyKey,
            String fingerprint,
            String walletId,
            Money money,
            String description,
            Instant occurredAt
    ) {
        WalletBalance currentBalance = balances.get(walletId);
        WalletBalance updatedBalance = new WalletBalance(walletId, currentBalance.money().add(money), occurredAt);
        balances.put(walletId, updatedBalance);

        String operationId = nextOperationId();
        addStepLog(operationId, OperationStep.BALANCE_LOCKED, occurredAt, "Balance locked for wallet " + walletId);
        addStepLog(operationId, OperationStep.BALANCE_UPDATED, occurredAt, "Balance updated for wallet " + walletId);

        TransactionHistoryItem transaction = transaction(
                walletId,
                occurredAt,
                TransactionType.CHARGE,
                TransactionDirection.CREDIT,
                money,
                description
        );
        transactions.computeIfAbsent(walletId, ignored -> new ArrayList<>()).add(transaction);
        addStepLog(operationId, OperationStep.TRANSACTION_RECORDED, occurredAt, "Transaction history recorded for wallet " + walletId);

        LedgerEntry ledgerEntry = ledgerEntry(
                operationId,
                walletId,
                occurredAt,
                TransactionType.CHARGE,
                TransactionDirection.CREDIT,
                money,
                updatedBalance.money(),
                description
        );
        ledgerEntries.computeIfAbsent(walletId, ignored -> new ArrayList<>()).add(ledgerEntry);
        addStepLog(operationId, OperationStep.LEDGER_RECORDED, occurredAt, "Ledger entry recorded for wallet " + walletId);
        auditEvents.add(auditEvent(
                operationId,
                AuditEventType.CHARGE_COMPLETED,
                occurredAt,
                "Charge completed for wallet " + walletId
        ));
        addStepLog(operationId, OperationStep.AUDIT_RECORDED, occurredAt, "Audit event recorded for operation " + operationId);

        WalletOperationResult result = new WalletOperationResult(
                operationId,
                transaction.transactionId(),
                walletId,
                null,
                occurredAt,
                TransactionType.CHARGE,
                TransactionStatus.COMPLETED,
                TransactionDirection.CREDIT,
                money,
                updatedBalance,
                description
        );
        WalletOperationRecord record = new WalletOperationRecord(idempotencyKey, fingerprint, result);
        operations.put(idempotencyKey, record);
        addStepLog(operationId, OperationStep.IDEMPOTENCY_RECORDED, occurredAt, "Idempotency record stored for operation " + operationId);
        addOutboxEvent(result);
        return record;
    }

    @Override
    public synchronized WalletOperationRecord applyTransfer(
            String idempotencyKey,
            String fingerprint,
            String sourceWalletId,
            String targetWalletId,
            Money money,
            String description,
            Instant occurredAt
    ) {
        WalletBalance sourceBalance = balances.get(sourceWalletId);
        WalletBalance targetBalance = balances.get(targetWalletId);
        String operationId = nextOperationId();
        addStepLog(operationId, OperationStep.BALANCE_LOCKED, occurredAt, "Balances locked for transfer " + sourceWalletId + " to " + targetWalletId);

        WalletBalance updatedSourceBalance = new WalletBalance(
                sourceWalletId,
                sourceBalance.money().subtract(money),
                occurredAt
        );
        WalletBalance updatedTargetBalance = new WalletBalance(
                targetWalletId,
                targetBalance.money().add(money),
                occurredAt
        );
        balances.put(sourceWalletId, updatedSourceBalance);
        balances.put(targetWalletId, updatedTargetBalance);
        addStepLog(operationId, OperationStep.BALANCE_UPDATED, occurredAt, "Balances updated for transfer " + sourceWalletId + " to " + targetWalletId);

        TransactionHistoryItem sourceTransaction = transaction(
                sourceWalletId,
                occurredAt,
                TransactionType.TRANSFER,
                TransactionDirection.DEBIT,
                money,
                description
        );
        TransactionHistoryItem targetTransaction = transaction(
                targetWalletId,
                occurredAt,
                TransactionType.TRANSFER,
                TransactionDirection.CREDIT,
                money,
                description
        );
        transactions.computeIfAbsent(sourceWalletId, ignored -> new ArrayList<>()).add(sourceTransaction);
        transactions.computeIfAbsent(targetWalletId, ignored -> new ArrayList<>()).add(targetTransaction);
        addStepLog(operationId, OperationStep.TRANSACTION_RECORDED, occurredAt, "Transaction history recorded for transfer " + sourceWalletId + " to " + targetWalletId);

        LedgerEntry sourceLedgerEntry = ledgerEntry(
                operationId,
                sourceWalletId,
                occurredAt,
                TransactionType.TRANSFER,
                TransactionDirection.DEBIT,
                money,
                updatedSourceBalance.money(),
                description
        );
        LedgerEntry targetLedgerEntry = ledgerEntry(
                operationId,
                targetWalletId,
                occurredAt,
                TransactionType.TRANSFER,
                TransactionDirection.CREDIT,
                money,
                updatedTargetBalance.money(),
                description
        );
        ledgerEntries.computeIfAbsent(sourceWalletId, ignored -> new ArrayList<>()).add(sourceLedgerEntry);
        ledgerEntries.computeIfAbsent(targetWalletId, ignored -> new ArrayList<>()).add(targetLedgerEntry);
        addStepLog(operationId, OperationStep.LEDGER_RECORDED, occurredAt, "Ledger entries recorded for transfer " + sourceWalletId + " to " + targetWalletId);
        auditEvents.add(auditEvent(
                operationId,
                AuditEventType.TRANSFER_COMPLETED,
                occurredAt,
                "Transfer completed from " + sourceWalletId + " to " + targetWalletId
        ));
        addStepLog(operationId, OperationStep.AUDIT_RECORDED, occurredAt, "Audit event recorded for operation " + operationId);

        WalletOperationResult result = new WalletOperationResult(
                operationId,
                sourceTransaction.transactionId(),
                sourceWalletId,
                targetWalletId,
                occurredAt,
                TransactionType.TRANSFER,
                TransactionStatus.COMPLETED,
                TransactionDirection.DEBIT,
                money,
                updatedSourceBalance,
                description
        );
        WalletOperationRecord record = new WalletOperationRecord(idempotencyKey, fingerprint, result);
        operations.put(idempotencyKey, record);
        addStepLog(operationId, OperationStep.IDEMPOTENCY_RECORDED, occurredAt, "Idempotency record stored for operation " + operationId);
        addOutboxEvent(result);
        return record;
    }

    private void addOutboxEvent(WalletOperationResult result) {
        operationOutboxEvents.computeIfAbsent(result.operationId(), ignored -> new ArrayList<>())
                .add(new OperationOutboxEvent(
                        nextOutboxEventId(),
                        result.operationId(),
                        result.type().name() + "_COMPLETED",
                        "WALLET_OPERATION",
                        result.operationId(),
                        outboxPayload(result),
                        OperationOutboxStatus.PENDING,
                        result.occurredAt(),
                        0,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
    }

    private boolean isReadyToClaim(OperationOutboxEvent outboxEvent, Instant now) {
        if (outboxEvent.status() == OperationOutboxStatus.PENDING) {
            return true;
        }
        if (outboxEvent.status() == OperationOutboxStatus.FAILED) {
            return outboxEvent.nextRetryAt() == null || !outboxEvent.nextRetryAt().isAfter(now);
        }
        return outboxEvent.status() == OperationOutboxStatus.PROCESSING
                && outboxEvent.leaseExpiresAt() != null
                && !outboxEvent.leaseExpiresAt().isAfter(now);
    }

    private OperationOutboxStatus failedStatus(OperationOutboxEvent event, int maxAttempts) {
        if (event.attemptCount() + 1 >= maxAttempts) {
            return OperationOutboxStatus.MANUAL_REVIEW;
        }
        return OperationOutboxStatus.FAILED;
    }

    private Instant failedNextRetryAt(OperationOutboxEvent event, Instant nextRetryAt, int maxAttempts) {
        if (event.attemptCount() + 1 >= maxAttempts) {
            return null;
        }
        return nextRetryAt;
    }

    private OperationOutboxEvent findOutboxEvent(String outboxEventId) {
        return operationOutboxEvents.values().stream()
                .flatMap(List::stream)
                .filter(outboxEvent -> outboxEvent.outboxEventId().equals(outboxEventId))
                .findFirst()
                .orElseThrow(() -> new InvalidWalletOperationException("manual review outbox event not found: " + outboxEventId));
    }

    private int compareOutboxEvents(OperationOutboxEvent left, OperationOutboxEvent right) {
        int occurredComparison = left.occurredAt().compareTo(right.occurredAt());
        if (occurredComparison != 0) {
            return occurredComparison;
        }
        return left.outboxEventId().compareTo(right.outboxEventId());
    }

    private OperationOutboxEvent processingOutboxEvent(
            OperationOutboxEvent event,
            Instant claimedAt,
            Instant leaseExpiresAt
    ) {
        return new OperationOutboxEvent(
                event.outboxEventId(),
                event.operationId(),
                event.eventType(),
                event.aggregateType(),
                event.aggregateId(),
                event.payload(),
                OperationOutboxStatus.PROCESSING,
                event.occurredAt(),
                event.attemptCount(),
                null,
                claimedAt,
                leaseExpiresAt,
                null,
                null
        );
    }

    private void replaceOutboxEvent(
            String outboxEventId,
            java.util.function.Function<OperationOutboxEvent, OperationOutboxEvent> replacer
    ) {
        operationOutboxEvents.replaceAll((operationId, events) -> {
            List<OperationOutboxEvent> replacedEvents = new ArrayList<>();
            for (OperationOutboxEvent event : events) {
                if (event.outboxEventId().equals(outboxEventId)) {
                    replacedEvents.add(replacer.apply(event));
                } else {
                    replacedEvents.add(event);
                }
            }
            return replacedEvents;
        });
    }

    private String outboxPayload(WalletOperationResult result) {
        return """
                {"operationId":"%s","walletId":"%s","counterpartyWalletId":%s,"type":"%s","amount":"%s","currency":"%s"}
                """.formatted(
                result.operationId(),
                result.walletId(),
                nullableJsonString(result.counterpartyWalletId()),
                result.type().name(),
                result.money().amount().stripTrailingZeros().toPlainString(),
                result.money().currency()
        ).trim();
    }

    private String nullableJsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value + "\"";
    }

    private void addStepLog(String operationId, OperationStep step, Instant occurredAt, String detail) {
        operationStepLogs.computeIfAbsent(operationId, ignored -> new ArrayList<>())
                .add(new OperationStepLog(
                        nextOperationStepLogId(),
                        operationId,
                        step,
                        TransactionStatus.COMPLETED,
                        occurredAt,
                        detail
                ));
    }

    private LedgerEntry ledgerEntry(
            String operationId,
            String walletId,
            Instant occurredAt,
            TransactionType type,
            TransactionDirection direction,
            Money money,
            Money balanceAfter,
            String description
    ) {
        return new LedgerEntry(
                nextLedgerEntryId(),
                operationId,
                walletId,
                occurredAt,
                type,
                direction,
                money,
                balanceAfter,
                description
        );
    }

    private AuditEvent auditEvent(
            String operationId,
            AuditEventType type,
            Instant occurredAt,
            String detail
    ) {
        return new AuditEvent(
                nextAuditEventId(),
                operationId,
                type,
                occurredAt,
                detail
        );
    }

    private TransactionHistoryItem transaction(
            String walletId,
            Instant occurredAt,
            TransactionType type,
            TransactionDirection direction,
            Money money,
            String description
    ) {
        return new TransactionHistoryItem(
                nextTransactionId(),
                walletId,
                occurredAt,
                type,
                TransactionStatus.COMPLETED,
                direction,
                money,
                description
        );
    }

    private String nextTransactionId() {
        transactionSequence += 1;
        return "txn-%03d".formatted(transactionSequence);
    }

    private String nextOperationId() {
        operationSequence += 1;
        return "op-%03d".formatted(operationSequence);
    }

    private String nextLedgerEntryId() {
        ledgerEntrySequence += 1;
        return "ledger-%03d".formatted(ledgerEntrySequence);
    }

    private String nextAuditEventId() {
        auditEventSequence += 1;
        return "audit-%03d".formatted(auditEventSequence);
    }

    private String nextOperationStepLogId() {
        operationStepLogSequence += 1;
        return "step-%03d".formatted(operationStepLogSequence);
    }

    private String nextOutboxEventId() {
        outboxEventSequence += 1;
        return "outbox-%03d".formatted(outboxEventSequence);
    }

    private String nextOutboxRequeueAuditId() {
        outboxRequeueAuditSequence += 1;
        return "outbox-requeue-audit-%03d".formatted(outboxRequeueAuditSequence);
    }
}
