package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class InMemoryWalletQueryServiceTest {

    private final InMemoryWalletQueryService service = new InMemoryWalletQueryService(
            Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void returnsBalanceAsOfCurrentClock() {
        assertThat(service.getBalance("wallet-001").asOf())
                .isEqualTo(Instant.parse("2026-05-01T00:00:00Z"));
    }

    @Test
    void rejectsBlankWalletId() {
        assertThatThrownBy(() -> service.getBalance(" "))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsUnknownWalletId() {
        assertThatThrownBy(() -> service.getBalance("unknown"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
