package com.imwoo.airepo.wallet.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.imwoo.airepo.wallet.application.WalletConcurrencyException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class WalletApiExceptionHandlerTest {

    private final WalletApiExceptionHandler exceptionHandler = new WalletApiExceptionHandler(
            Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void mapsWalletConcurrencyToBusyConflict() {
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleWalletConcurrency(
                new WalletConcurrencyException("Wallet balance is busy. Please retry.", new RuntimeException())
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(new ApiErrorResponse(
                "WALLET_BALANCE_BUSY",
                "Wallet balance is busy. Please retry.",
                Instant.parse("2026-05-01T00:00:00Z")
        ));
    }
}
