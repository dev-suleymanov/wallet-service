package com.example.wallet_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresWalletRepository implements WalletRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresWalletRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public Optional<BigDecimal> updateBalance(UUID id, BigDecimal delta) {
        final String sql = """
            UPDATE wallet
               SET balance = balance + ?
             WHERE id = ?
               AND (balance + ?) >= 0
            RETURNING balance
            """;
        List<BigDecimal> out = jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setBigDecimal(1, delta);
            ps.setObject(2, id);
            ps.setBigDecimal(3, delta);
            return ps;
        }, (rs, rn) -> rs.getBigDecimal(1));
        return out.isEmpty() ? Optional.empty() : Optional.of(out.get(0));
    }

    @Override
    public Optional<BigDecimal> getBalance(UUID id) {
        final String sql = "SELECT balance FROM wallet WHERE id = ?";
        List<BigDecimal> result = jdbcTemplate.query(sql,
                ps -> ps.setObject(1, id),
                (rs, rn) -> rs.getBigDecimal("balance"));
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public boolean exists(UUID id) {
        final String sql = "SELECT EXISTS(SELECT 1 FROM wallet WHERE id = ?)";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, id);
        return Boolean.TRUE.equals(exists);
    }
}
