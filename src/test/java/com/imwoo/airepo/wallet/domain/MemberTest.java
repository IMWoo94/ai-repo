package com.imwoo.airepo.wallet.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    void rejectsBlankMemberId() {
        assertThatThrownBy(() -> new Member(" ", MemberStatus.ACTIVE, Instant.parse("2026-05-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("memberId must not be blank");
    }
}
