package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.Member;
import com.imwoo.airepo.wallet.domain.MemberStatus;
import com.imwoo.airepo.wallet.domain.OperationOutboxStatus;
import com.imwoo.airepo.wallet.domain.OperationStep;
import com.imwoo.airepo.wallet.domain.TransactionDirection;
import com.imwoo.airepo.wallet.domain.TransactionType;
import com.imwoo.airepo.wallet.domain.WalletAccount;
import com.imwoo.airepo.wallet.domain.WalletAccountStatus;
import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryWalletLedgerQueryServiceTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final InMemoryWalletCommandService commandService = new InMemoryWalletCommandService(
            Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
            repository
    );
    private final InMemoryWalletLedgerQueryService ledgerQueryService = new InMemoryWalletLedgerQueryService(
            repository,
            repository
    );

    @Test
    void returnsLedgerEntriesByLatestFirst() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        commandService.transfer(
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("25000"), "transfer-001", "테스트 송금")
        );

        assertThat(ledgerQueryService.getLedgerEntries("wallet-001"))
                .hasSize(2)
                .satisfies(entries -> {
                    assertThat(entries.get(0).type()).isEqualTo(TransactionType.TRANSFER);
                    assertThat(entries.get(0).direction()).isEqualTo(TransactionDirection.DEBIT);
                    assertThat(entries.get(0).balanceAfter()).isEqualTo(money("105000"));
                    assertThat(entries.get(1).type()).isEqualTo(TransactionType.CHARGE);
                    assertThat(entries.get(1).direction()).isEqualTo(TransactionDirection.CREDIT);
                    assertThat(entries.get(1).balanceAfter()).isEqualTo(money("130000"));
                });
    }

    @Test
    void idempotentRetryDoesNotCreateDuplicatedLedgerEntry() {
        WalletChargeCommand command = new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전");

        WalletCommandResult first = commandService.charge("wallet-001", command);
        commandService.charge("wallet-001", command);

        assertThat(ledgerQueryService.getLedgerEntries("wallet-001")).hasSize(1);
        assertThat(ledgerQueryService.getAuditEvents()).hasSize(1);
        assertThat(ledgerQueryService.getOperationStepLogs(first.operation().operationId())).hasSize(6);
        assertThat(ledgerQueryService.getOperationOutboxEvents(first.operation().operationId())).hasSize(1);
    }

    @Test
    void returnsAuditEventsByLatestFirst() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        commandService.transfer(
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("25000"), "transfer-001", "테스트 송금")
        );

        assertThat(ledgerQueryService.getAuditEvents())
                .hasSize(2)
                .satisfies(events -> {
                    assertThat(events.get(0).type().name()).isEqualTo("TRANSFER_COMPLETED");
                    assertThat(events.get(1).type().name()).isEqualTo("CHARGE_COMPLETED");
                });
    }

    @Test
    void returnsOperationStepLogsByProcessOrder() {
        WalletCommandResult result = commandService.charge(
                "wallet-001",
                new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전")
        );

        assertThat(ledgerQueryService.getOperationStepLogs(result.operation().operationId()))
                .extracting(stepLog -> stepLog.step())
                .containsExactly(
                        OperationStep.BALANCE_LOCKED,
                        OperationStep.BALANCE_UPDATED,
                        OperationStep.TRANSACTION_RECORDED,
                        OperationStep.LEDGER_RECORDED,
                        OperationStep.AUDIT_RECORDED,
                        OperationStep.IDEMPOTENCY_RECORDED
                );
    }

    @Test
    void returnsPendingOutboxEventAfterCharge() {
        WalletCommandResult result = commandService.charge(
                "wallet-001",
                new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전")
        );

        assertThat(ledgerQueryService.getOperationOutboxEvents(result.operation().operationId()))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.eventType()).isEqualTo("CHARGE_COMPLETED");
                    assertThat(outboxEvent.aggregateType()).isEqualTo("WALLET_OPERATION");
                    assertThat(outboxEvent.aggregateId()).isEqualTo(result.operation().operationId());
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PENDING);
                    assertThat(outboxEvent.payload()).contains("\"operationId\":\"op-001\"");
                });
    }

    @Test
    void rejectsLedgerQueryWhenWalletSuspended() {
        SuspendedWalletRepository suspendedRepository = new SuspendedWalletRepository();
        InMemoryWalletLedgerQueryService service = new InMemoryWalletLedgerQueryService(
                suspendedRepository,
                suspendedRepository
        );

        assertThatThrownBy(() -> service.getLedgerEntries("wallet-suspended"))
                .isInstanceOf(WalletAccountNotQueryableException.class)
                .hasMessage("Wallet is not queryable: wallet-suspended");
    }

    @Test
    void rejectsLedgerQueryWhenWalletClosed() {
        ClosedWalletRepository closedRepository = new ClosedWalletRepository();
        InMemoryWalletLedgerQueryService service = new InMemoryWalletLedgerQueryService(
                closedRepository,
                closedRepository
        );

        assertThatThrownBy(() -> service.getLedgerEntries("wallet-closed"))
                .isInstanceOf(WalletAccountNotQueryableException.class)
                .hasMessage("Wallet is not queryable: wallet-closed");
    }

    @Test
    void rejectsLedgerQueryWhenOwnerMemberSuspended() {
        SuspendedOwnerRepository suspendedOwnerRepository = new SuspendedOwnerRepository();
        InMemoryWalletLedgerQueryService service = new InMemoryWalletLedgerQueryService(
                suspendedOwnerRepository,
                suspendedOwnerRepository
        );

        assertThatThrownBy(() -> service.getLedgerEntries("wallet-owner-suspended"))
                .isInstanceOf(WalletAccountNotQueryableException.class)
                .hasMessage("Wallet is not queryable: wallet-owner-suspended");
    }

    @Test
    void transferCreatesLedgerEntryOnTargetWallet() {
        commandService.transfer(
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("25000"), "transfer-001", "테스트 송금")
        );

        assertThat(ledgerQueryService.getLedgerEntries("wallet-002"))
                .singleElement()
                .satisfies(ledgerEntry -> {
                    assertThat(ledgerEntry.type()).isEqualTo(TransactionType.TRANSFER);
                    assertThat(ledgerEntry.direction()).isEqualTo(TransactionDirection.CREDIT);
                    assertThat(ledgerEntry.balanceAfter()).isEqualTo(money("55000"));
                });
    }

    @Test
    void rejectsUnknownOperationStepLogQuery() {
        assertThatThrownBy(() -> ledgerQueryService.getOperationStepLogs("op-9999"))
                .isInstanceOf(OperationNotFoundException.class)
                .hasMessage("Operation not found: op-9999");
    }

    @Test
    void rejectsUnknownOperationOutboxEventQuery() {
        assertThatThrownBy(() -> ledgerQueryService.getOperationOutboxEvents("op-9999"))
                .isInstanceOf(OperationNotFoundException.class)
                .hasMessage("Operation not found: op-9999");
    }

    @Test
    void rejectsUnknownWalletLedgerQuery() {
        assertThatThrownBy(() -> ledgerQueryService.getLedgerEntries("unknown"))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessage("Wallet not found: unknown");
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), "KRW");
    }

    private static class SuspendedWalletRepository extends InMemoryWalletRepository {

        @Override
        public Optional<WalletAccount> findWalletAccount(String walletId) {
            if ("wallet-suspended".equals(walletId)) {
                return Optional.of(new WalletAccount(
                        "wallet-suspended",
                        "member-001",
                        WalletAccountStatus.SUSPENDED,
                        Instant.parse("2026-05-01T00:00:00Z")
                ));
            }
            return super.findWalletAccount(walletId);
        }
    }

    private static class ClosedWalletRepository extends InMemoryWalletRepository {

        @Override
        public Optional<WalletAccount> findWalletAccount(String walletId) {
            if ("wallet-closed".equals(walletId)) {
                return Optional.of(new WalletAccount(
                        "wallet-closed",
                        "member-001",
                        WalletAccountStatus.CLOSED,
                        Instant.parse("2026-05-01T00:00:00Z")
                ));
            }
            return super.findWalletAccount(walletId);
        }
    }

    private static class SuspendedOwnerRepository extends InMemoryWalletRepository {

        @Override
        public Optional<Member> findMember(String memberId) {
            if ("member-suspended".equals(memberId)) {
                return Optional.of(new Member(
                        "member-suspended",
                        MemberStatus.SUSPENDED,
                        Instant.parse("2026-05-01T00:00:00Z")
                ));
            }
            return super.findMember(memberId);
        }

        @Override
        public Optional<WalletAccount> findWalletAccount(String walletId) {
            if ("wallet-owner-suspended".equals(walletId)) {
                return Optional.of(new WalletAccount(
                        "wallet-owner-suspended",
                        "member-suspended",
                        WalletAccountStatus.ACTIVE,
                        Instant.parse("2026-05-01T00:00:00Z")
                ));
            }
            return super.findWalletAccount(walletId);
        }
    }
}
