package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.WalletAccount;
import com.imwoo.airepo.wallet.domain.WalletAccountStatus;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import com.imwoo.airepo.wallet.infra.InMemoryWalletQueryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryWalletQueryServiceTest {

    private final InMemoryWalletQueryService service = new InMemoryWalletQueryService(
            Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
            new InMemoryWalletQueryRepository()
    );

    @Test
    void returnsBalanceAsOfCurrentClock() {
        assertThat(service.getBalance("wallet-001").asOf())
                .isEqualTo(Instant.parse("2026-05-01T00:00:00Z"));
    }

    @Test
    void rejectsBlankWalletId() {
        assertThatThrownBy(() -> service.getBalance(" "))
                .isInstanceOf(InvalidWalletIdException.class)
                .hasMessage("walletId must not be blank");
    }

    @Test
    void rejectsUnknownWalletId() {
        assertThatThrownBy(() -> service.getBalance("unknown"))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessage("Wallet not found: unknown");
    }

    @Test
    void rejectsSuspendedWalletAccount() {
        InMemoryWalletQueryService suspendedWalletService = new InMemoryWalletQueryService(
                Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
                new SuspendedWalletRepository()
        );

        assertThatThrownBy(() -> suspendedWalletService.getBalance("wallet-suspended"))
                .isInstanceOf(WalletAccountNotQueryableException.class)
                .hasMessage("Wallet is not queryable: wallet-suspended");
    }

    private static class SuspendedWalletRepository extends InMemoryWalletQueryRepository {

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

        @Override
        public Optional<WalletBalance> findBalance(String walletId) {
            return Optional.empty();
        }
    }
}
