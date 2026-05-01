package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.TransactionDirection;
import com.imwoo.airepo.wallet.domain.TransactionHistoryItem;
import com.imwoo.airepo.wallet.domain.TransactionStatus;
import com.imwoo.airepo.wallet.domain.TransactionType;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InMemoryWalletQueryService implements WalletQueryService {

    private static final String DEFAULT_CURRENCY = "KRW";

    private final Clock clock;
    private final Map<String, WalletBalance> balances;
    private final Map<String, List<TransactionHistoryItem>> transactions;

    public InMemoryWalletQueryService(Clock clock) {
        this.clock = clock;
        this.balances = Map.of(
                "wallet-001",
                new WalletBalance("wallet-001", new Money(new BigDecimal("125000"), DEFAULT_CURRENCY), Instant.parse("2026-05-01T00:00:00Z"))
        );
        this.transactions = Map.of(
                "wallet-001",
                List.of(
                        new TransactionHistoryItem(
                                "txn-002",
                                "wallet-001",
                                Instant.parse("2026-05-01T00:00:00Z"),
                                TransactionType.REWARD,
                                TransactionStatus.COMPLETED,
                                TransactionDirection.CREDIT,
                                new Money(new BigDecimal("25000"), DEFAULT_CURRENCY),
                                "학습용 리워드 적립"
                        ),
                        new TransactionHistoryItem(
                                "txn-001",
                                "wallet-001",
                                Instant.parse("2026-04-30T00:00:00Z"),
                                TransactionType.CHARGE,
                                TransactionStatus.COMPLETED,
                                TransactionDirection.CREDIT,
                                new Money(new BigDecimal("100000"), DEFAULT_CURRENCY),
                                "학습용 충전"
                        )
                )
        );
    }

    @Override
    public WalletBalance getBalance(String walletId) {
        WalletBalance balance = balances.get(walletId);
        if (balance == null) {
            throw walletNotFound(walletId);
        }
        return new WalletBalance(balance.walletId(), balance.money(), Instant.now(clock));
    }

    @Override
    public List<TransactionHistoryItem> getTransactions(String walletId) {
        if (!balances.containsKey(walletId)) {
            throw walletNotFound(walletId);
        }
        return transactions.getOrDefault(walletId, List.of()).stream()
                .sorted(Comparator.comparing(TransactionHistoryItem::occurredAt).reversed())
                .toList();
    }

    private ResponseStatusException walletNotFound(String walletId) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found: " + walletId);
    }
}
