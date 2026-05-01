package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class StepLogFailurePolicyTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final InMemoryWalletCommandService commandService = new InMemoryWalletCommandService(
            Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
            repository
    );

    @Test
    void insufficientBalanceTransferDoesNotLeaveStepLogOrOutboxEvent() {
        assertThatThrownBy(() -> commandService.transfer(
                "wallet-002",
                new WalletTransferCommand("wallet-001", money("30001"), "transfer-fail-001", "잔액 부족 송금")
        )).isInstanceOf(InsufficientBalanceException.class);

        assertThat(allStoredStepLogCount()).isZero();
        assertThat(allStoredOutboxEventCount()).isZero();
    }

    @Test
    void zeroAmountChargeDoesNotLeaveStepLogOrOutboxEvent() {
        assertThatThrownBy(() -> commandService.charge(
                "wallet-001",
                new WalletChargeCommand(money("0"), "charge-fail-zero", "0원 충전")
        )).isInstanceOf(InvalidWalletOperationException.class);

        assertThat(allStoredStepLogCount()).isZero();
        assertThat(allStoredOutboxEventCount()).isZero();
    }

    @Test
    void unsupportedCurrencyChargeDoesNotLeaveStepLogOrOutboxEvent() {
        assertThatThrownBy(() -> commandService.charge(
                "wallet-001",
                new WalletChargeCommand(new Money(new BigDecimal("1000"), "USD"), "charge-fail-usd", "USD 충전")
        )).isInstanceOf(InvalidWalletOperationException.class);

        assertThat(allStoredStepLogCount()).isZero();
        assertThat(allStoredOutboxEventCount()).isZero();
    }

    @Test
    void idempotencyConflictRetryDoesNotLeaveAdditionalStepLogOrOutboxEvent() {
        commandService.charge(
                "wallet-001",
                new WalletChargeCommand(money("5000"), "charge-conflict", "정상 충전")
        );
        long firstStepLogCount = allStoredStepLogCount();
        long firstOutboxEventCount = allStoredOutboxEventCount();

        assertThatThrownBy(() -> commandService.charge(
                "wallet-001",
                new WalletChargeCommand(money("6000"), "charge-conflict", "다른 금액")
        )).isInstanceOf(IdempotencyKeyConflictException.class);

        assertThat(allStoredStepLogCount()).isEqualTo(firstStepLogCount);
        assertThat(allStoredOutboxEventCount()).isEqualTo(firstOutboxEventCount);
    }

    private long allStoredStepLogCount() {
        long total = 0;
        for (int index = 1; index <= 100; index++) {
            total += repository.findOperationStepLogs(operationId(index)).size();
        }
        return total;
    }

    private long allStoredOutboxEventCount() {
        long total = 0;
        for (int index = 1; index <= 100; index++) {
            total += repository.findOperationOutboxEvents(operationId(index)).size();
        }
        return total;
    }

    private String operationId(int index) {
        return "op-%03d".formatted(index);
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), "KRW");
    }
}
