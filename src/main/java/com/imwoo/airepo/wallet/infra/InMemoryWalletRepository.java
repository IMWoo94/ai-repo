package com.imwoo.airepo.wallet.infra;

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
public class InMemoryWalletRepository implements WalletCommandRepository, WalletLedgerQueryRepository {

    private static final String DEFAULT_CURRENCY = "KRW";

    private final Map<String, Member> members = new HashMap<>();
    private final Map<String, WalletAccount> walletAccounts = new HashMap<>();
    private final Map<String, WalletBalance> balances = new HashMap<>();
    private final Map<String, List<TransactionHistoryItem>> transactions = new HashMap<>();
    private final Map<String, List<LedgerEntry>> ledgerEntries = new HashMap<>();
    private final List<AuditEvent> auditEvents = new ArrayList<>();
    private final Map<String, WalletOperationRecord> operations = new HashMap<>();
    private int transactionSequence = 2;
    private int operationSequence = 0;
    private int ledgerEntrySequence = 0;
    private int auditEventSequence = 0;

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

        TransactionHistoryItem transaction = transaction(
                walletId,
                occurredAt,
                TransactionType.CHARGE,
                TransactionDirection.CREDIT,
                money,
                description
        );
        transactions.computeIfAbsent(walletId, ignored -> new ArrayList<>()).add(transaction);
        String operationId = nextOperationId();
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
        auditEvents.add(auditEvent(
                operationId,
                AuditEventType.CHARGE_COMPLETED,
                occurredAt,
                "Charge completed for wallet " + walletId
        ));

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
        String operationId = nextOperationId();
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
        auditEvents.add(auditEvent(
                operationId,
                AuditEventType.TRANSFER_COMPLETED,
                occurredAt,
                "Transfer completed from " + sourceWalletId + " to " + targetWalletId
        ));

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
        return record;
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
}
