package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.WalletAccount;
import com.imwoo.airepo.wallet.domain.WalletAccountStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class WalletAccessPolicyTest {

    private WalletAccount wallet(String walletId, String memberId) {
        return new WalletAccount(walletId, memberId, WalletAccountStatus.ACTIVE, Instant.parse("2026-05-01T00:00:00Z"));
    }

    @Test
    void allowsOwnerAccess() {
        assertThatCode(() -> WalletAccessPolicy.requireOwnership(wallet("wallet-001", "member-001"), "member-001"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNonOwnerAccess() {
        assertThatThrownBy(() -> WalletAccessPolicy.requireOwnership(wallet("wallet-001", "member-001"), "member-002"))
                .isInstanceOf(WalletAccessDeniedException.class);
    }
}
