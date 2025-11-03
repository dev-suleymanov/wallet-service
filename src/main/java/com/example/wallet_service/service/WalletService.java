package com.example.wallet_service.service;

import com.example.wallet_service.exception.WalletException;
import com.example.wallet_service.repository.WalletRepository;
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
            throw WalletException.walletNotFound();
        }
        return walletRepository.updateBalance(walletId, delta)
                .orElseThrow(WalletException::insufficientFunds);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID walletId) {
        return walletRepository.getBalance(walletId)
                .orElseThrow(WalletException::walletNotFound);
    }
}
