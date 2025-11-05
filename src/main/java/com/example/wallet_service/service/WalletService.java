package com.example.wallet_service.service;

import com.example.wallet_service.exception.ApiException;
import com.example.wallet_service.repository.WalletRepository;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class WalletService {

    private static final int MAX_RETRIES = 5;
    private static final Set<String> RETRY_STATES = Set.of("40001", "40P01");

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public BigDecimal updateBalance(UUID walletId, BigDecimal delta) {
        if (!walletRepository.exists(walletId)) {
            throw ApiException.walletNotFound(walletId.toString());
        }
        if (delta.signum() < 0) {
            BigDecimal current = walletRepository.getBalance(walletId)
                    .orElseThrow(() -> ApiException.internal("Balance not found during update"));
            if (current.add(delta).compareTo(BigDecimal.ZERO) < 0) {
                throw ApiException.insufficientFunds();
            }
        }
        try {
            Optional<BigDecimal> after = walletRepository.updateBalance(walletId, delta);
            if (after.isPresent()) {
                return after.get();
            }
            if (!walletRepository.exists(walletId)) {
                throw ApiException.walletNotFound(walletId.toString());
            }
            throw ApiException.insufficientFunds();
        } catch (OptimisticLockingFailureException | CannotAcquireLockException e) {
            throw ApiException.concurrency();
        } catch (DataAccessException e) {
            throw ApiException.internal("Database error");
        }
    }


    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID walletId) {
        if (!walletRepository.exists(walletId)) {
            throw ApiException.walletNotFound(walletId.toString());
        }
        return walletRepository.getBalance(walletId)
                .orElseThrow(() -> ApiException.internal("Balance not found"));
    }

    private void backoff(int attempt) {
        long ms = Math.min(80, 5L << (attempt - 1));
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }
}
