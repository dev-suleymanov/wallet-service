package com.example.wallet_service;

import com.example.wallet_service.repository.WalletRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WalletConcurrencyTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    WalletRepository walletRepository;

    private static final UUID WALLET_ID = UUID.fromString("b6d6d54c-9f25-4dc6-8a9f-9d9b1b9f1f11");

    private final AtomicReference<BigDecimal> balance = new AtomicReference<>(new BigDecimal("10000.00"));

    @BeforeAll
    static void enforceLocale() {
        Locale.setDefault(Locale.US);
    }

    @BeforeEach
    void setupRepoMock() {
        when(walletRepository.exists(eq(WALLET_ID))).thenReturn(true);
        when(walletRepository.getBalance(eq(WALLET_ID))).thenAnswer((Answer<Optional<BigDecimal>>) invocation ->
                Optional.of(balance.get()));
        when(walletRepository.updateBalance(eq(WALLET_ID), any(BigDecimal.class)))
                .thenAnswer((Answer<Optional<BigDecimal>>) invocation -> {
                    BigDecimal delta = invocation.getArgument(1);
                    for (;;) {
                        BigDecimal cur = balance.get();
                        BigDecimal next = cur.add(delta);
                        if (next.compareTo(BigDecimal.ZERO) < 0) {
                            return Optional.empty();
                        }
                        if (balance.compareAndSet(cur, next)) {
                            return Optional.of(next);
                        }
                    }
                });
    }

    private static BigDecimal randomAmount() {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0.01, 10.00))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Test
    @DisplayName("Тест конкурентности: 1000 запросов/сек к одному кошельку, без 5xx ошибок")
    void hotWallet_noServerErrors() throws Exception {
        final int durationSeconds = 10;
        final int rps = 1000;
        final int total = durationSeconds * rps;
        final int poolSize = 300;
        ExecutorService exec = Executors.newFixedThreadPool(poolSize);
        CompletionService<Void> cs = new ExecutorCompletionService<>(exec);
        AtomicInteger serverErrors = new AtomicInteger(0);
        AtomicInteger ok2xx = new AtomicInteger(0);
        AtomicInteger expected4xx = new AtomicInteger(0);
        mockMvc.perform(get("/api/v1/wallets/" + WALLET_ID))
                .andReturn();
        long start = System.nanoTime();
        for (int i = 0; i < total; i++) {
            cs.submit(() -> {
                try {
                    boolean deposit = ThreadLocalRandom.current().nextBoolean();
                    BigDecimal amount = randomAmount();
                    String body = """
                            {
                              "walletId": "%s",
                              "operationType": "%s",
                              "amount": %s
                            }
                            """.formatted(WALLET_ID, deposit ? "DEPOSIT" : "WITHDRAW", amount.toPlainString());
                    int status = mockMvc.perform(post("/api/v1/wallet")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                            .andReturn().getResponse().getStatus();
                    if (status >= 500) serverErrors.incrementAndGet();
                    else if (status >= 200 && status < 300) ok2xx.incrementAndGet();
                    else if (status == 400 || status == 404 || status == 409) expected4xx.incrementAndGet();
                } catch (Exception e) {
                    serverErrors.incrementAndGet();
                }
                return null;
            });
            busyWait(start, i + 1, rps);
        }
        for (int i = 0; i < total; i++) cs.take();
        exec.shutdown();
        exec.awaitTermination(30, TimeUnit.SECONDS);
        assertThat(serverErrors.get())
                .as("Ни один запрос не должен вернуть 5xx")
                .isZero();
        assertThat(balance.get().compareTo(BigDecimal.ZERO))
                .as("Баланс не должен быть отрицательным")
                .isGreaterThanOrEqualTo(0);
    }

    private static void busyWait(long start, int sent, int rps) {
        long target = (long) (sent / (double) rps * 1_000_000_000L);
        while (System.nanoTime() - start < target) {
            Thread.onSpinWait();
        }
    }
}
