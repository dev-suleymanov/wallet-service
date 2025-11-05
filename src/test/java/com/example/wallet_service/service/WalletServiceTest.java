package com.example.wallet_service.service;

import com.example.wallet_service.exception.ApiException;
import com.example.wallet_service.exception.ErrorType;
import com.example.wallet_service.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    WalletRepository walletRepository;

    @InjectMocks
    WalletService walletService;

    @Test
    @DisplayName("updateBalance: NOT FOUND если кошелька нет")
    void updateBalance_walletNotFound() {
        UUID id = UUID.randomUUID();
        when(walletRepository.exists(id)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> walletService.updateBalance(id, new BigDecimal("10.00")));

        assertThat(ex.getStatus().value()).isEqualTo(404);
        assertThat(ex.getType()).isEqualTo(ErrorType.BUSINESS);
        assertThat(ex.getSubtype().name()).isEqualTo("WALLET_NOT_FOUND");
        verify(walletRepository).exists(id);
        verifyNoMoreInteractions(walletRepository);
    }

    @Test
    @DisplayName("updateBalance: WITHDRAW -> недостаточно средств")
    void updateBalance_withdraw_insufficientFunds() {
        UUID id = UUID.randomUUID();
        when(walletRepository.exists(id)).thenReturn(true);
        when(walletRepository.getBalance(id)).thenReturn(Optional.of(new BigDecimal("100.00")));

        ApiException ex = assertThrows(ApiException.class,
                () -> walletService.updateBalance(id, new BigDecimal("-200.00")));

        assertThat(ex.getStatus().value()).isEqualTo(409);
        assertThat(ex.getSubtype().name()).isEqualTo("INSUFFICIENT_FUNDS");
        InOrder inOrder = inOrder(walletRepository);
        inOrder.verify(walletRepository).exists(id);
        inOrder.verify(walletRepository).getBalance(id);
        verify(walletRepository, never()).updateBalance(any(), any());
    }

    @Test
    @DisplayName("updateBalance: DEPOSIT -> успешное обновление")
    void updateBalance_deposit_success() {
        UUID id = UUID.randomUUID();
        when(walletRepository.exists(id)).thenReturn(true);
        when(walletRepository.updateBalance(id, new BigDecimal("50.00")))
                .thenReturn(Optional.of(new BigDecimal("150.00")));

        BigDecimal result = walletService.updateBalance(id, new BigDecimal("50.00"));

        assertThat(result).isEqualByComparingTo("150.00");
        verify(walletRepository).exists(id);
        verify(walletRepository, never()).getBalance(any());
        verify(walletRepository).updateBalance(id, new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("updateBalance: WITHDRAW -> успешное обновление")
    void updateBalance_withdraw_success() {
        UUID id = UUID.randomUUID();
        when(walletRepository.exists(id)).thenReturn(true);
        when(walletRepository.getBalance(id)).thenReturn(Optional.of(new BigDecimal("200.00")));
        when(walletRepository.updateBalance(id, new BigDecimal("-50.00")))
                .thenReturn(Optional.of(new BigDecimal("150.00")));

        BigDecimal result = walletService.updateBalance(id, new BigDecimal("-50.00"));

        assertThat(result).isEqualByComparingTo("150.00");
        InOrder inOrder = inOrder(walletRepository);
        inOrder.verify(walletRepository).exists(id);
        inOrder.verify(walletRepository).getBalance(id);
        inOrder.verify(walletRepository).updateBalance(id, new BigDecimal("-50.00"));
    }

    @Test
    @DisplayName("updateBalance: Optional.empty() → INSUFFICIENT_FUNDS (409)")
    void updateBalance_updateReturnedEmpty() {
        UUID id = UUID.randomUUID();
        when(walletRepository.exists(id)).thenReturn(true);
        when(walletRepository.getBalance(id)).thenReturn(Optional.of(new BigDecimal("100.00")));
        when(walletRepository.updateBalance(id, new BigDecimal("-10.00")))
                .thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class,
                () -> walletService.updateBalance(id, new BigDecimal("-10.00")));
        assertThat(ex.getStatus().value()).isEqualTo(409);
        assertThat(ex.getSubtype().name()).isEqualTo("INSUFFICIENT_FUNDS");
        InOrder inOrder = inOrder(walletRepository);
        inOrder.verify(walletRepository).exists(id);
        inOrder.verify(walletRepository).getBalance(id);
        inOrder.verify(walletRepository).updateBalance(id, new BigDecimal("-10.00"));
    }


    @Test
    @DisplayName("updateBalance: OptimisticLockingFailure -> CONCURRENCY_ERROR (409)")
    void updateBalance_optimisticLockingMapped() {
        UUID id = UUID.randomUUID();
        when(walletRepository.exists(id)).thenReturn(true);
        when(walletRepository.updateBalance(id, new BigDecimal("1.00")))
                .thenThrow(new OptimisticLockingFailureException("boom"));

        ApiException ex = assertThrows(ApiException.class,
                () -> walletService.updateBalance(id, new BigDecimal("1.00")));

        assertThat(ex.getStatus().value()).isEqualTo(409);
        assertThat(ex.getSubtype().name()).isEqualTo("CONCURRENCY_ERROR");
    }

    @Test
    @DisplayName("updateBalance: CannotAcquireLock -> CONCURRENCY_ERROR (409)")
    void updateBalance_cannotAcquireLockMapped() {
        UUID id = UUID.randomUUID();
        when(walletRepository.exists(id)).thenReturn(true);
        when(walletRepository.updateBalance(id, new BigDecimal("1.00")))
                .thenThrow(new CannotAcquireLockException("db lock"));

        ApiException ex = assertThrows(ApiException.class,
                () -> walletService.updateBalance(id, new BigDecimal("1.00")));

        assertThat(ex.getStatus().value()).isEqualTo(409);
        assertThat(ex.getSubtype().name()).isEqualTo("CONCURRENCY_ERROR");
    }

    @Test
    @DisplayName("updateBalance: WITHDRAW -> INTERNAL_ERROR если баланс не найден")
    void updateBalance_withdraw_balanceNotFound() {
        UUID id = UUID.randomUUID();
        when(walletRepository.exists(id)).thenReturn(true);
        when(walletRepository.getBalance(id)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> walletService.updateBalance(id, new BigDecimal("-1.00")));

        assertThat(ex.getStatus().value()).isEqualTo(500);
        assertThat(ex.getSubtype().name()).isEqualTo("INTERNAL_ERROR");
        verify(walletRepository, never()).updateBalance(any(), any());
    }

    @Test
    @DisplayName("getBalance: NOT FOUND если кошелька нет")
    void getBalance_walletNotFound() {
        UUID id = UUID.randomUUID();
        when(walletRepository.exists(id)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> walletService.getBalance(id));

        assertThat(ex.getStatus().value()).isEqualTo(404);
        assertThat(ex.getSubtype().name()).isEqualTo("WALLET_NOT_FOUND");
        verify(walletRepository).exists(id);
        verifyNoMoreInteractions(walletRepository);
    }

    @Test
    @DisplayName("getBalance: успешное получение баланса")
    void getBalance_success() {
        UUID id = UUID.randomUUID();
        when(walletRepository.exists(id)).thenReturn(true);
        when(walletRepository.getBalance(id)).thenReturn(Optional.of(new BigDecimal("250.00")));

        BigDecimal result = walletService.getBalance(id);

        assertThat(result).isEqualByComparingTo("250.00");
        verify(walletRepository).exists(id);
        verify(walletRepository).getBalance(id);
    }

    @Test
    @DisplayName("getBalance: Optional.empty() -> INTERNAL_ERROR")
    void getBalance_internalWhenEmpty() {
        UUID id = UUID.randomUUID();
        when(walletRepository.exists(id)).thenReturn(true);
        when(walletRepository.getBalance(id)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> walletService.getBalance(id));

        assertThat(ex.getStatus().value()).isEqualTo(500);
        assertThat(ex.getSubtype().name()).isEqualTo("INTERNAL_ERROR");
    }
}
