package com.example.wallet_service.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {
    boolean exists(UUID id);
    Optional<BigDecimal> getBalance(UUID id);
    Optional<BigDecimal> updateBalance(UUID id, BigDecimal delta);
}