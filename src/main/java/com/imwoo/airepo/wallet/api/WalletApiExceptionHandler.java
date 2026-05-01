package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.InvalidWalletIdException;
import com.imwoo.airepo.wallet.application.IdempotencyKeyConflictException;
import com.imwoo.airepo.wallet.application.InsufficientBalanceException;
import com.imwoo.airepo.wallet.application.InvalidWalletOperationException;
import com.imwoo.airepo.wallet.application.OperationNotFoundException;
import com.imwoo.airepo.wallet.application.WalletAccountNotQueryableException;
import com.imwoo.airepo.wallet.application.WalletConcurrencyException;
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

    @ExceptionHandler(OperationNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleOperationNotFound(OperationNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "OPERATION_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(WalletAccountNotQueryableException.class)
    ResponseEntity<ApiErrorResponse> handleWalletAccountNotQueryable(WalletAccountNotQueryableException exception) {
        return error(HttpStatus.CONFLICT, "WALLET_NOT_QUERYABLE", exception.getMessage());
    }

    @ExceptionHandler(InvalidWalletOperationException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidWalletOperation(InvalidWalletOperationException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_WALLET_OPERATION", exception.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    ResponseEntity<ApiErrorResponse> handleInsufficientBalance(InsufficientBalanceException exception) {
        return error(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE", exception.getMessage());
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    ResponseEntity<ApiErrorResponse> handleIdempotencyKeyConflict(IdempotencyKeyConflictException exception) {
        return error(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(WalletConcurrencyException.class)
    ResponseEntity<ApiErrorResponse> handleWalletConcurrency(WalletConcurrencyException exception) {
        return error(HttpStatus.CONFLICT, "WALLET_BALANCE_BUSY", exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(code, message, Instant.now(clock)));
    }
}
