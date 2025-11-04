package com.example.wallet_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletOperationRequest (
        UUID walletId,
        OperationType operationType,
        BigDecimal amount
) {
    public enum OperationType {
        DEPOSIT,
        WITHDRAW
    }
}
