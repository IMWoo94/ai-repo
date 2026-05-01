package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.WalletQueryService;
import com.imwoo.airepo.wallet.domain.TransactionHistoryItem;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletQueryController {

    private final WalletQueryService walletQueryService;

    public WalletQueryController(WalletQueryService walletQueryService) {
        this.walletQueryService = walletQueryService;
    }

    @GetMapping("/{walletId}/balance")
    public WalletBalance balance(@PathVariable String walletId) {
        return walletQueryService.getBalance(walletId);
    }

    @GetMapping("/{walletId}/transactions")
    public List<TransactionHistoryItem> transactions(@PathVariable String walletId) {
        return walletQueryService.getTransactions(walletId);
    }
}
