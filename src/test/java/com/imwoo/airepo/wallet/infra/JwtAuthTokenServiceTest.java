package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.application.AuthToken;
import com.imwoo.airepo.wallet.application.AuthTokenProperties;
import com.imwoo.airepo.wallet.application.MemberNotActiveException;
import com.imwoo.airepo.wallet.application.MemberNotFoundException;
import com.imwoo.airepo.wallet.domain.Member;
import com.imwoo.airepo.wallet.domain.MemberStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JwtAuthTokenServiceTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-32b";
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);

    private JwtAuthTokenService service(MemberStatus status) {
        return new JwtAuthTokenService(
                new AuthTokenProperties(SECRET, 60),
                clock,
                memberId -> status == null
                        ? Optional.empty()
                        : Optional.of(new Member(memberId, status, Instant.parse("2026-05-01T00:00:00Z")))
        );
    }

    @Test
    void issuesSignedTokenForActiveMember() {
        AuthToken token = service(MemberStatus.ACTIVE).issueToken("member-001");

        assertThat(token.memberId()).isEqualTo("member-001");
        assertThat(token.token()).isNotBlank();
        assertThat(token.expiresAt()).isEqualTo(Instant.parse("2026-05-01T01:00:00Z"));
    }

    @Test
    void rejectsUnknownMember() {
        assertThatThrownBy(() -> service(null).issueToken("ghost"))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void rejectsInactiveMember() {
        assertThatThrownBy(() -> service(MemberStatus.SUSPENDED).issueToken("member-001"))
                .isInstanceOf(MemberNotActiveException.class);
    }
}
