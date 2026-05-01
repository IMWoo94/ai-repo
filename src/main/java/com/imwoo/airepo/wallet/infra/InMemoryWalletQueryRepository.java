package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.WalletQueryRepository;
import com.imwoo.airepo.wallet.domain.Member;
import com.imwoo.airepo.wallet.domain.MemberStatus;
import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.TransactionDirection;
import com.imwoo.airepo.wallet.domain.TransactionHistoryItem;
import com.imwoo.airepo.wallet.domain.TransactionStatus;
import com.imwoo.airepo.wallet.domain.TransactionType;
import com.imwoo.airepo.wallet.domain.WalletAccount;
import com.imwoo.airepo.wallet.domain.WalletAccountStatus;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryWalletQueryRepository implements WalletQueryRepository {

    private static final String DEFAULT_CURRENCY = "KRW";

    private final Map<String, Member> members;
    private final Map<String, WalletAccount> walletAccounts;
    private final Map<String, WalletBalance> balances;
    private final Map<String, List<TransactionHistoryItem>> transactions;

    public InMemoryWalletQueryRepository() {
        this.members = Map.of(
                "member-001",
                new Member("member-001", MemberStatus.ACTIVE, Instant.parse("2026-05-01T00:00:00Z"))
        );
        this.walletAccounts = Map.of(
                "wallet-001",
                new WalletAccount(
                        "wallet-001",
                        "member-001",
                        WalletAccountStatus.ACTIVE,
                        Instant.parse("2026-05-01T00:00:00Z")
                )
        );
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
    public Optional<WalletAccount> findWalletAccount(String walletId) {
        return Optional.ofNullable(walletAccounts.get(walletId));
    }

    @Override
    public Optional<WalletBalance> findBalance(String walletId) {
        return Optional.ofNullable(balances.get(walletId));
    }

    @Override
    public List<TransactionHistoryItem> findTransactions(String walletId) {
        return transactions.getOrDefault(walletId, List.of());
    }

    public Optional<Member> findMember(String memberId) {
        return Optional.ofNullable(members.get(memberId));
    }
}
