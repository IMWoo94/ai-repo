package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.InvalidWalletIdException;
import com.imwoo.airepo.wallet.application.WalletNotFoundException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WalletApiExceptionHandler {

    private final Clock clock;

    public WalletApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(InvalidWalletIdException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidWalletId(InvalidWalletIdException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_WALLET_ID", exception.getMessage());
    }

    @ExceptionHandler(WalletNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleWalletNotFound(WalletNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(code, message, Instant.now(clock)));
    }
}
