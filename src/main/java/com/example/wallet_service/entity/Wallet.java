package com.example.wallet_service.entity;

import java.math.BigDecimal;
import java.util.UUID;

public record Wallet(
        UUID id,
        BigDecimal balance
) {}
