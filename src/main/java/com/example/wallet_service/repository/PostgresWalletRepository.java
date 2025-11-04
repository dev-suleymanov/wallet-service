package com.example.wallet_service.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgresWalletRepository implements WalletRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public PostgresWalletRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean exists(UUID id) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "select exists(select 1 from wallet where id = :id)",
                        Map.of("id", id),
                        Boolean.class
                )
        );
    }

    @Override
    public Optional<BigDecimal> getBalance(UUID id) {
        return jdbc.query(
                "select balance from wallet where id = :id",
                Map.of("id", id),
                (rs, i) -> rs.getBigDecimal("balance")
        ).stream().findFirst();
    }

    @Override
    public Optional<BigDecimal> updateBalance(UUID id, BigDecimal delta) {
        return jdbc.query(
                """
                update wallet
                   set balance = balance + :delta,
                       version = version + 1,
                       updated_at = CURRENT_TIMESTAMP
                 where id = :id
                   and (balance + :delta) >= 0
             returning balance
                """,
                Map.of("id", id, "delta", delta),
                (rs, i) -> rs.getBigDecimal("balance")
        ).stream().findFirst();
    }
}