package com.example.wallet_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletBalanceResponse(
        UUID walletId,
        BigDecimal balance
) {}
