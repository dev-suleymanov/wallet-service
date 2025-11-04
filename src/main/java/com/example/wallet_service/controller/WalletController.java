package com.example.wallet_service.controller;

import com.example.wallet_service.dto.WalletBalanceResponse;
import com.example.wallet_service.dto.WalletOperationRequest;
import com.example.wallet_service.exception.ApiException;
import com.example.wallet_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletBalanceResponse> updateBalance(@RequestBody WalletOperationRequest request) {
        validateRequest(request);
        BigDecimal delta = switch (request.operationType()) {
            case DEPOSIT -> request.amount();
            case WITHDRAW -> request.amount().negate();
        };
        BigDecimal updatedBalance = walletService.updateBalance(request.walletId(), delta);
        WalletBalanceResponse response = new WalletBalanceResponse(request.walletId(), updatedBalance);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletBalanceResponse> getBalance(@PathVariable UUID walletId) {
        validateWalletId(walletId);
        BigDecimal balance = walletService.getBalance(walletId);
        return ResponseEntity.ok(new WalletBalanceResponse(walletId, balance));
    }

    private void validateRequest(WalletOperationRequest request) {
        if (request == null) {
            throw ApiException.validation("Request body must not be null");
        }
        validateWalletId(request.walletId());
        if (request.operationType() == null) {
            throw ApiException.validation("operationType must not be null");
        }
        if (request.amount() == null) {
            throw ApiException.validation("amount must not be null");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw ApiException.validation("amount must not be negative");
        }
        if (request.amount().compareTo(new BigDecimal("0.01")) < 0) {
            throw ApiException.validation("amount must be at least 0.01");
        }
        if (request.amount().scale() > 2) {
            throw ApiException.validation("amount must have at most 2 decimal places");
        }
        if (request.amount().precision() - request.amount().scale() > 17) {
            throw ApiException.validation("amount must have at most 17 integer digits");
        }
    }

    private void validateWalletId(UUID walletId) {
        if (walletId == null || walletId.toString().isEmpty()) {
            throw ApiException.validation("walletId must not be null");
        }
    }
}
