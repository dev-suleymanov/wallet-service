package com.example.wallet_service.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {
    Optional<BigDecimal> updateBalance(UUID id, BigDecimal delta);
    Optional<BigDecimal> getBalance(UUID id);
    boolean exists(UUID id);
}
