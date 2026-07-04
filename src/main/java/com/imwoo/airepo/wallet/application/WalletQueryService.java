package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.TransactionHistoryItem;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import java.util.List;

public interface WalletQueryService {

    WalletBalance getBalance(String memberId, String walletId);

    List<TransactionHistoryItem> getTransactions(String memberId, String walletId);
}
