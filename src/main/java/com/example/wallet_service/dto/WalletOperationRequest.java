package com.example.wallet_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletOperationRequest (
        @NotNull UUID walletId,
        @NotNull OperationType operationType,
        @DecimalMin(value = "0.01")
        @Digits(integer = 18, fraction = 2)
        BigDecimal amount
) {
    public enum OperationType {
        DEPOSIT,
        WITHDRAW
    }
}
