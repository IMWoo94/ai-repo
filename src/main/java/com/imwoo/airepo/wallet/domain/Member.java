package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record Member(String memberId, MemberStatus status, Instant createdAt) {

    public Member {
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (memberId.isBlank()) {
            throw new IllegalArgumentException("memberId must not be blank");
        }
    }

    public boolean active() {
        return status == MemberStatus.ACTIVE;
    }
}
