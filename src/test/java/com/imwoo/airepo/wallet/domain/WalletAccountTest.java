package com.imwoo.airepo.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class WalletAccountTest {

    @Test
    void activeWalletIsQueryable() {
        WalletAccount walletAccount = new WalletAccount(
                "wallet-001",
                "member-001",
                WalletAccountStatus.ACTIVE,
                Instant.parse("2026-05-01T00:00:00Z")
        );

        assertThat(walletAccount.queryable()).isTrue();
    }

    @Test
    void suspendedWalletIsNotQueryable() {
        WalletAccount walletAccount = new WalletAccount(
                "wallet-001",
                "member-001",
                WalletAccountStatus.SUSPENDED,
                Instant.parse("2026-05-01T00:00:00Z")
        );

        assertThat(walletAccount.queryable()).isFalse();
    }

    @Test
    void closedWalletIsNotQueryable() {
        WalletAccount walletAccount = new WalletAccount(
                "wallet-001",
                "member-001",
                WalletAccountStatus.CLOSED,
                Instant.parse("2026-05-01T00:00:00Z")
        );

        assertThat(walletAccount.queryable()).isFalse();
    }

    @Test
    void rejectsBlankMemberId() {
        assertThatThrownBy(() -> new WalletAccount(
                "wallet-001",
                " ",
                WalletAccountStatus.ACTIVE,
                Instant.parse("2026-05-01T00:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("memberId must not be blank");
    }
}
