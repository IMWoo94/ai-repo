package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.TransactionDirection;
import com.imwoo.airepo.wallet.domain.TransactionType;
import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

        commandService.charge("wallet-001", command);
        commandService.charge("wallet-001", command);

        assertThat(ledgerQueryService.getLedgerEntries("wallet-001")).hasSize(1);
        assertThat(ledgerQueryService.getAuditEvents()).hasSize(1);
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
    void rejectsUnknownWalletLedgerQuery() {
        assertThatThrownBy(() -> ledgerQueryService.getLedgerEntries("unknown"))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessage("Wallet not found: unknown");
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), "KRW");
    }
}
