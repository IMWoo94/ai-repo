package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JdbcOutboxRelayRepositoryLastErrorTest {

    @Test
    void keepsNullLastError() {
        assertThat(JdbcOutboxRelayRepository.truncateLastError(null)).isNull();
    }

    @Test
    void keepsLastErrorWithinColumnLimit() {
        String message = "a".repeat(255);
        assertThat(JdbcOutboxRelayRepository.truncateLastError(message)).isEqualTo(message);
    }

    @Test
    void truncatesLastErrorExceedingColumnLimit() {
        String message = "a".repeat(300);
        assertThat(JdbcOutboxRelayRepository.truncateLastError(message)).hasSize(255);
    }
}
