package com.example.wallet_service.controller;

import com.example.wallet_service.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WalletControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    WalletService walletService;

    private static final String BASE = "/api/v1/wallets";

    private static String asJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number) {
                sb.append(v);
            } else {
                sb.append("\"").append(v.toString()).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static ResultMatcher jsonBigDecimal(String path, String expected) {
        return result -> {
            String content = result.getResponse().getContentAsString();
            Object actual = com.jayway.jsonpath.JsonPath.read(content, path);
            BigDecimal actualDecimal = new BigDecimal(actual.toString());
            assertThat(actualDecimal).isEqualByComparingTo(expected);
        };
    }

    @Nested
    @DisplayName("POST /api/v1/wallets — updateBalance")
    class UpdateBalance {

        @Test
        @DisplayName("DEPOSIT: 100.00 → delta положительная, 200 OK и корректный ответ")
        void depositOk() throws Exception {
            UUID walletId = UUID.randomUUID();
            BigDecimal amount = new BigDecimal("100.00");

            ArgumentCaptor<BigDecimal> deltaCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            when(walletService.updateBalance(eq(walletId), deltaCaptor.capture()))
                    .thenReturn(new BigDecimal("350.00"));

            var body = asJson(Map.of(
                    "walletId", walletId.toString(),
                    "operationType", "DEPOSIT",
                    "amount", amount
            ));

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                    .andExpect(jsonBigDecimal("$.balance", "350.00"));

            assertThat(deltaCaptor.getValue()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("WITHDRAW: 25.50 → delta отрицательная, 200 OK и корректный ответ")
        void withdrawOk() throws Exception {
            UUID walletId = UUID.randomUUID();
            BigDecimal amount = new BigDecimal("25.50");

            ArgumentCaptor<BigDecimal> deltaCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            when(walletService.updateBalance(eq(walletId), deltaCaptor.capture()))
                    .thenReturn(new BigDecimal("74.50"));

            var body = asJson(Map.of(
                    "walletId", walletId.toString(),
                    "operationType", "WITHDRAW",
                    "amount", amount
            ));

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                    .andExpect(jsonBigDecimal("$.balance", "74.50"));

            assertThat(deltaCaptor.getValue()).isEqualByComparingTo("-25.50");
        }

        @Test
        @DisplayName("null body → 400")
        void nullBody() throws Exception {
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("walletId = null → 400")
        void nullWalletId() throws Exception {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("walletId", null);
            body.put("operationType", "DEPOSIT");
            body.put("amount", 10);
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(asJson(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("operationType = null → 400")
        void nullOperationType() throws Exception {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("walletId", UUID.randomUUID().toString());
            body.put("operationType", null);
            body.put("amount", 10);
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(asJson(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("amount = null → 400")
        void nullAmount() throws Exception {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("walletId", UUID.randomUUID().toString());
            body.put("operationType", "DEPOSIT");
            body.put("amount", null);
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(asJson(body)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("amount < 0 → 400")
        void negativeAmount() throws Exception {
            UUID walletId = UUID.randomUUID();
            var body = asJson(Map.of(
                    "walletId", walletId.toString(),
                    "operationType", "DEPOSIT",
                    "amount", -1
            ));
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest(name = "amount={0} (меньше 0.01) → 400")
        @ValueSource(strings = {"0.0", "0.001", "0.009", "0.00"})
        void belowMinAmount(String amount) throws Exception {
            UUID walletId = UUID.randomUUID();
            var body = asJson(Map.of(
                    "walletId", walletId.toString(),
                    "operationType", "DEPOSIT",
                    "amount", amount
            ));
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("amount с >2 знаками после запятой → 400")
        void tooManyFractionDigits() throws Exception {
            UUID walletId = UUID.randomUUID();
            var body = asJson(Map.of(
                    "walletId", walletId.toString(),
                    "operationType", "DEPOSIT",
                    "amount", "10.123"
            ));
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("amount с >17 целыми цифрами → 400")
        void tooManyIntegerDigits() throws Exception {
            UUID walletId = UUID.randomUUID();
            var body = asJson(Map.of(
                    "walletId", walletId.toString(),
                    "operationType", "DEPOSIT",
                    "amount", "100000000000000000.00"
            ));
            mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/wallets/{walletId} — getBalance")
    class GetBalance {

        @Test
        @DisplayName("валидный UUID → 200 OK и тело ответа")
        void getOk() throws Exception {
            UUID walletId = UUID.randomUUID();
            when(walletService.getBalance(walletId)).thenReturn(new BigDecimal("123.45"));

            mockMvc.perform(get(BASE + "/" + walletId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                    .andExpect(jsonBigDecimal("$.balance", "123.45"));
        }

        @Test
        @DisplayName("невалидный UUID (тип-конверсия) → 400")
        void invalidUuidInPath() throws Exception {
            mockMvc.perform(get(BASE + "/not-a-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }
}
