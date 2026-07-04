package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.AuthToken;
import com.imwoo.airepo.wallet.application.AuthTokenProperties;
import com.imwoo.airepo.wallet.application.AuthTokenService;
import com.imwoo.airepo.wallet.application.MemberLookup;
import com.imwoo.airepo.wallet.application.MemberNotActiveException;
import com.imwoo.airepo.wallet.application.MemberNotFoundException;
import com.imwoo.airepo.wallet.domain.Member;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class JwtAuthTokenService implements AuthTokenService {

    private final AuthTokenProperties properties;
    private final Clock clock;
    private final MemberLookup memberLookup;

    public JwtAuthTokenService(AuthTokenProperties properties, Clock clock, MemberLookup memberLookup) {
        this.properties = properties;
        this.clock = clock;
        this.memberLookup = memberLookup;
    }

    @Override
    public AuthToken issueToken(String memberId) {
        Member member = memberLookup.findMember(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        if (!member.active()) {
            throw new MemberNotActiveException(memberId);
        }
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(Duration.ofMinutes(properties.ttlMinutes()));
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    new JWTClaimsSet.Builder()
                            .subject(member.memberId())
                            .issueTime(Date.from(now))
                            .expirationTime(Date.from(expiresAt))
                            .build()
            );
            jwt.sign(new MACSigner(properties.secret().getBytes(StandardCharsets.UTF_8)));
            return new AuthToken(jwt.serialize(), member.memberId(), expiresAt);
        } catch (JOSEException exception) {
            throw new IllegalStateException("Failed to sign auth token", exception);
        }
    }
}
