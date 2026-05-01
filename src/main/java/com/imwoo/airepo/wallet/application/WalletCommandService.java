package com.imwoo.airepo.wallet.application;

public interface WalletCommandService {

    WalletCommandResult charge(String walletId, WalletChargeCommand command);

    WalletCommandResult transfer(String sourceWalletId, WalletTransferCommand command);
}
