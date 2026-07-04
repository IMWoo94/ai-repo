package com.imwoo.airepo.wallet.application;

public interface WalletCommandService {

    WalletCommandResult charge(String memberId, String walletId, WalletChargeCommand command);

    WalletCommandResult transfer(String memberId, String sourceWalletId, WalletTransferCommand command);
}
