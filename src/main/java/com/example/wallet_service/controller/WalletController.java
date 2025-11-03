package com.example.wallet_service.controller;

import com.example.wallet_service.dto.WalletBalanceResponse;
import com.example.wallet_service.dto.WalletOperationRequest;
import com.example.wallet_service.exception.WalletException;
import com.example.wallet_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletBalanceResponse> updateBalance(@RequestBody WalletOperationRequest request) {
        validateRequest(request);
        BigDecimal delta = request.operationType() == WalletOperationRequest.OperationType.DEPOSIT
                ? request.amount()
                : request.amount().negate();

        BigDecimal updatedBalance = walletService.updateBalance(request.walletId(), delta);
        WalletBalanceResponse response = new WalletBalanceResponse(request.walletId(), updatedBalance, Instant.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletBalanceResponse> getBalance(@PathVariable UUID walletId) {
        BigDecimal balance = walletService.getBalance(walletId);
        return ResponseEntity.ok(new WalletBalanceResponse(walletId, balance, Instant.now()));
    }

    private void validateRequest(WalletOperationRequest request) {
        if (request.walletId() == null || request.amount() == null || request.amount().signum() <= 0) {
            throw WalletException.invalidRequest();
        }
        if (request.operationType() == null) {
            throw WalletException.invalidRequest();
        }
    }
}
