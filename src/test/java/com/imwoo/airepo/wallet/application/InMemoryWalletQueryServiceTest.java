package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.Member;
import com.imwoo.airepo.wallet.domain.MemberStatus;
import com.imwoo.airepo.wallet.domain.WalletAccount;
import com.imwoo.airepo.wallet.domain.WalletAccountStatus;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryWalletQueryServiceTest {

    private final InMemoryWalletQueryService service = new InMemoryWalletQueryService(
            Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
            new InMemoryWalletRepository()
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

    @Test
    void rejectsClosedWalletAccount() {
        InMemoryWalletQueryService closedWalletService = new InMemoryWalletQueryService(
                Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
                new ClosedWalletRepository()
        );

        assertThatThrownBy(() -> closedWalletService.getBalance("wallet-closed"))
                .isInstanceOf(WalletAccountNotQueryableException.class)
                .hasMessage("Wallet is not queryable: wallet-closed");
    }

    @Test
    void rejectsWalletWhenOwnerMemberIsSuspended() {
        InMemoryWalletQueryService inactiveOwnerService = new InMemoryWalletQueryService(
                Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
                new SuspendedOwnerRepository()
        );

        assertThatThrownBy(() -> inactiveOwnerService.getBalance("wallet-owner-suspended"))
                .isInstanceOf(WalletAccountNotQueryableException.class)
                .hasMessage("Wallet is not queryable: wallet-owner-suspended");
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

        @Override
        public Optional<WalletBalance> findBalance(String walletId) {
            return Optional.empty();
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

        @Override
        public Optional<WalletBalance> findBalance(String walletId) {
            return Optional.empty();
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

        @Override
        public Optional<WalletBalance> findBalance(String walletId) {
            return Optional.empty();
        }
    }
}
