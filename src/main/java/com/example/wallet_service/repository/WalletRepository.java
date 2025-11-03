package com.example.wallet_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class WalletRepository {
    private final JdbcTemplate jdbcTemplate;

    public WalletRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

    }

    public Optional<BigDecimal> updateBalance(UUID id, BigDecimal delta) {
        String sql = """
                    UPDATE wallet
                       SET balance = balance + ?, updated_at = now()
                     WHERE id = ? AND balance + ? >= 0
                 RETURNING balance
                """;
        List<BigDecimal> result = jdbcTemplate.query(sql, ps -> {
            ps.setBigDecimal(1, delta);
            ps.setObject(2, id);
            ps.setBigDecimal(3, delta);
        }, (rs, rn) -> rs.getBigDecimal(1));
        return result.isEmpty()
                ? Optional.empty()
                : Optional.of(result.get(0));
    }

    public Optional<BigDecimal> getBalance(UUID id) {
        String sql = "SELECT balance FROM wallet WHERE id = ?";
        List<BigDecimal> result = jdbcTemplate.query(sql, ps -> {
            ps.setObject(1, id);
        }, (rs, rn) -> rs.getBigDecimal("balance"));
        return result.isEmpty()
                ? Optional.empty()
                : Optional.of(result.get(0));
    }

    public boolean exists(UUID id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM wallet WHERE id = ?)";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, id);
        return Boolean.TRUE.equals(exists);
    }
}
