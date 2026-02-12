package com.example.stockmonitor.repository;

import com.example.stockmonitor.model.StockPrice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PriceRepository {

    private final JdbcTemplate jdbcTemplate;

    public PriceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(StockPrice price) {
        jdbcTemplate.update("""
            INSERT INTO stock_prices(symbol, price, timestamp)
            VALUES (?, ?, ?)
        """, price.getSymbol(), price.getPrice(), price.getTimestamp());
    }

    public List<StockPrice> findLatestPrices() {
        return jdbcTemplate.query("""
            SELECT symbol, price, timestamp
            FROM stock_prices
        """, (rs, rowNum) ->
                new StockPrice(
                        rs.getString("symbol"),
                        rs.getDouble("price"),
                        rs.getTimestamp("timestamp").toLocalDateTime()
                )
        );
    }
}

