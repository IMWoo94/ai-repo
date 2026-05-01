package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.TransactionHistoryItem;
import com.imwoo.airepo.wallet.domain.WalletAccount;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import java.util.List;
import java.util.Optional;

public interface WalletQueryRepository {

    Optional<WalletAccount> findWalletAccount(String walletId);

    Optional<WalletBalance> findBalance(String walletId);

    List<TransactionHistoryItem> findTransactions(String walletId);
}
