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

class InMemoryWalletCommandServiceTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final InMemoryWalletCommandService service = new InMemoryWalletCommandService(
            Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
            repository
    );

    @Test
    void chargeIncreasesBalanceAndRecordsTransaction() {
        WalletCommandResult result = service.charge(
                "member-001",
                "wallet-001",
                new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전")
        );

        assertThat(result.created()).isTrue();
        assertThat(result.operation().type()).isEqualTo(TransactionType.CHARGE);
        assertThat(result.operation().direction()).isEqualTo(TransactionDirection.CREDIT);
        assertThat(result.operation().balance().money()).isEqualTo(money("130000"));
        assertThat(repository.findTransactions("wallet-001"))
                .anySatisfy(transaction -> assertThat(transaction.transactionId())
                        .isEqualTo(result.operation().transactionId()));
    }

    @Test
    void sameChargeIdempotencyKeyReturnsExistingResultWithoutIncreasingBalanceAgain() {
        WalletChargeCommand command = new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전");

        WalletCommandResult first = service.charge("member-001", "wallet-001", command);
        WalletCommandResult second = service.charge("member-001", "wallet-001", command);

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.operation().transactionId()).isEqualTo(first.operation().transactionId());
        assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("130000"));
    }

    @Test
    void sameIdempotencyKeyWithDifferentChargeRequestFails() {
        service.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));

        assertThatThrownBy(() -> service.charge(
                "member-001",
                "wallet-001",
                new WalletChargeCommand(money("6000"), "charge-001", "테스트 충전")
        ))
                .isInstanceOf(IdempotencyKeyConflictException.class)
                .hasMessage("Idempotency key already used for different request: charge-001");
    }

    @Test
    void transferMovesMoneyBetweenWalletsAndRecordsTransactions() {
        WalletCommandResult result = service.transfer(
                "member-001",
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("25000"), "transfer-001", "테스트 송금")
        );

        assertThat(result.created()).isTrue();
        assertThat(result.operation().type()).isEqualTo(TransactionType.TRANSFER);
        assertThat(result.operation().direction()).isEqualTo(TransactionDirection.DEBIT);
        assertThat(result.operation().counterpartyWalletId()).isEqualTo("wallet-002");
        assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("100000"));
        assertThat(repository.findBalance("wallet-002").orElseThrow().money()).isEqualTo(money("55000"));
        assertThat(repository.findTransactions("wallet-001"))
                .anySatisfy(transaction -> assertThat(transaction.direction()).isEqualTo(TransactionDirection.DEBIT));
        assertThat(repository.findTransactions("wallet-002"))
                .anySatisfy(transaction -> assertThat(transaction.direction()).isEqualTo(TransactionDirection.CREDIT));
    }

    @Test
    void sameTransferIdempotencyKeyReturnsExistingResultEvenWhenBalanceDrainedToZero() {
        WalletTransferCommand command =
                new WalletTransferCommand("wallet-002", money("125000"), "transfer-001", "테스트 송금");

        WalletCommandResult first = service.transfer("member-001", "wallet-001", command);
        assertThat(first.created()).isTrue();
        assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("0"));

        WalletCommandResult second = service.transfer("member-001", "wallet-001", command);

        assertThat(second.created()).isFalse();
        assertThat(second.operation().transactionId()).isEqualTo(first.operation().transactionId());
        assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("0"));
    }

    @Test
    void transferRejectsInsufficientBalance() {
        assertThatThrownBy(() -> service.transfer(
                "member-002",
                "wallet-002",
                new WalletTransferCommand("wallet-001", money("30001"), "transfer-001", "테스트 송금")
        ))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance: wallet-002");
    }

    @Test
    void transferRejectsSameSourceAndTargetWallet() {
        assertThatThrownBy(() -> service.transfer(
                "member-001",
                "wallet-001",
                new WalletTransferCommand("wallet-001", money("1000"), "transfer-001", "테스트 송금")
        ))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("sourceWalletId and targetWalletId must be different");
    }

    @Test
    void chargeRejectsWhenMemberDoesNotOwnWallet() {
        assertThatThrownBy(() -> service.charge(
                "member-002",
                "wallet-001",
                new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전")
        ))
                .isInstanceOf(WalletAccessDeniedException.class);
    }

    @Test
    void transferRejectsWhenMemberDoesNotOwnSourceWallet() {
        assertThatThrownBy(() -> service.transfer(
                "member-002",
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("1000"), "transfer-001", "테스트 송금")
        ))
                .isInstanceOf(WalletAccessDeniedException.class);
    }

    @Test
    void chargeRejectsZeroAmount() {
        assertThatThrownBy(() -> service.charge(
                "member-001",
                "wallet-001",
                new WalletChargeCommand(money("0"), "charge-001", "테스트 충전")
        ))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("amount must be positive");
    }

    @Test
    void chargeRejectsUnsupportedCurrency() {
        assertThatThrownBy(() -> service.charge(
                "member-001",
                "wallet-001",
                new WalletChargeCommand(new Money(new BigDecimal("1000"), "USD"), "charge-001", "테스트 충전")
        ))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("currency must be KRW");
    }

    @Test
    void differentWalletsMayReuseSameIdempotencyKeyWithoutConflict() {
        WalletCommandResult first = service.charge(
                "member-001",
                "wallet-001",
                new WalletChargeCommand(money("5000"), "shared-key", "테스트 충전")
        );
        WalletCommandResult second = service.charge(
                "member-002",
                "wallet-002",
                new WalletChargeCommand(money("7000"), "shared-key", "테스트 충전")
        );

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isTrue();
        assertThat(second.operation().operationId()).isNotEqualTo(first.operation().operationId());
        assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("130000"));
        assertThat(repository.findBalance("wallet-002").orElseThrow().money()).isEqualTo(money("37000"));
    }

    @Test
    void sameWalletAndKeyWithDifferentFingerprintStillConflicts() {
        service.charge("member-001", "wallet-001", new WalletChargeCommand(money("5000"), "shared-key", "테스트 충전"));

        assertThatThrownBy(() -> service.charge(
                "member-001",
                "wallet-001",
                new WalletChargeCommand(money("6000"), "shared-key", "테스트 충전")
        ))
                .isInstanceOf(IdempotencyKeyConflictException.class)
                .hasMessage("Idempotency key already used for different request: shared-key");
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), "KRW");
    }
}
