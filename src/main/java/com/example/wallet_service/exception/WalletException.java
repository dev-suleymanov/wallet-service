package com.example.wallet_service.exception;

import org.springframework.http.HttpStatus;

public class WalletException extends ApiException {

    private WalletException(HttpStatus status, ErrorCode code, String message) {
        super(status, code, message);
    }

    public static WalletException walletNotFound() {
        return new WalletException(
                HttpStatus.NOT_FOUND,
                ErrorCode.WALLET_NOT_FOUND,
                "Wallet not found"
        );
    }

    public static WalletException insufficientFunds() {
        return new WalletException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INSUFFICIENT_FUNDS,
                "Insufficient funds"
        );
    }
}
