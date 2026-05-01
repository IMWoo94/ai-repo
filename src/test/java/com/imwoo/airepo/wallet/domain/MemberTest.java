package com.imwoo.airepo.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    void activeMemberIsActive() {
        Member member = new Member("member-001", MemberStatus.ACTIVE, Instant.parse("2026-05-01T00:00:00Z"));

        assertThat(member.active()).isTrue();
    }

    @Test
    void suspendedMemberIsNotActive() {
        Member member = new Member("member-001", MemberStatus.SUSPENDED, Instant.parse("2026-05-01T00:00:00Z"));

        assertThat(member.active()).isFalse();
    }

    @Test
    void rejectsBlankMemberId() {
        assertThatThrownBy(() -> new Member(" ", MemberStatus.ACTIVE, Instant.parse("2026-05-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("memberId must not be blank");
    }
}
