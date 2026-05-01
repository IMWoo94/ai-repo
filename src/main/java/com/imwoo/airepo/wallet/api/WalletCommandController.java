package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.WalletChargeCommand;
import com.imwoo.airepo.wallet.application.WalletCommandResult;
import com.imwoo.airepo.wallet.application.WalletCommandService;
import com.imwoo.airepo.wallet.application.InvalidWalletOperationException;
import com.imwoo.airepo.wallet.application.WalletOperationResult;
import com.imwoo.airepo.wallet.application.WalletTransferCommand;
import com.imwoo.airepo.wallet.domain.Money;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletCommandController {

    private final WalletCommandService walletCommandService;

    public WalletCommandController(WalletCommandService walletCommandService) {
        this.walletCommandService = walletCommandService;
    }

    @PostMapping("/{walletId}/charges")
    public ResponseEntity<WalletOperationResult> charge(
            @PathVariable String walletId,
            @RequestBody WalletChargeRequest request
    ) {
        WalletCommandResult result = walletCommandService.charge(
                walletId,
                new WalletChargeCommand(
                        money(request.amount(), request.currency()),
                        required("idempotencyKey", request.idempotencyKey()),
                        required("description", request.description())
                )
        );
        return ResponseEntity.status(status(result)).body(result.operation());
    }

    @PostMapping("/{walletId}/transfers")
    public ResponseEntity<WalletOperationResult> transfer(
            @PathVariable String walletId,
            @RequestBody WalletTransferRequest request
    ) {
        WalletCommandResult result = walletCommandService.transfer(
                walletId,
                new WalletTransferCommand(
                        required("targetWalletId", request.targetWalletId()),
                        money(request.amount(), request.currency()),
                        required("idempotencyKey", request.idempotencyKey()),
                        required("description", request.description())
                )
        );
        return ResponseEntity.status(status(result)).body(result.operation());
    }

    private HttpStatus status(WalletCommandResult result) {
        if (result.created()) {
            return HttpStatus.CREATED;
        }
        return HttpStatus.OK;
    }

    private Money money(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new InvalidWalletOperationException("amount must not be null");
        }
        if (amount.signum() < 0) {
            throw new InvalidWalletOperationException("amount must be positive");
        }
        String requiredCurrency = required("currency", currency);
        if (requiredCurrency.isBlank()) {
            throw new InvalidWalletOperationException("currency must not be blank");
        }
        return new Money(amount, requiredCurrency);
    }

    private String required(String fieldName, String value) {
        if (value == null) {
            throw new InvalidWalletOperationException(fieldName + " must not be null");
        }
        return value;
    }
}
