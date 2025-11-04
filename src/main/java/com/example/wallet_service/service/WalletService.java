package com.example.wallet_service.service;

import com.example.wallet_service.exception.ApiException;
import com.example.wallet_service.repository.WalletRepository;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public BigDecimal updateBalance(UUID walletId, BigDecimal delta) {
        if (!walletRepository.exists(walletId)) {
            throw ApiException.walletNotFound(walletId.toString());
        }
        try {
            if (delta.signum() < 0) {
                BigDecimal current = walletRepository.getBalance(walletId)
                        .orElseThrow(() -> ApiException.internal("Balance not found during update"));
                if (current.add(delta).compareTo(BigDecimal.ZERO) < 0) {
                    throw ApiException.insufficientFunds();
                }
            }
            return walletRepository.updateBalance(walletId, delta)
                    .orElseThrow(() -> ApiException.internal("Failed to update balance"));
        } catch (OptimisticLockingFailureException | CannotAcquireLockException e) {
            throw ApiException.concurrency();
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
}
