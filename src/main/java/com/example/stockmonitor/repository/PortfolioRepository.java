package com.example.stockmonitor.repository;

import com.example.stockmonitor.model.Holding;
import com.example.stockmonitor.model.Portfolio;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PortfolioRepository {

    private final JdbcTemplate jdbcTemplate;

    public PortfolioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Portfolio> findAll() {
        String sql = """
                SELECT id, user_id, name
                FROM portfolios
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, (rs, _) -> {
            Portfolio portfolio = new Portfolio();
            portfolio.setId(rs.getLong("id"));
            portfolio.setUserId(rs.getLong("user_id"));
            portfolio.setName(rs.getString("name"));
            portfolio.setHoldings(new ArrayList<>());
            return portfolio;
        });
    }

    public Optional<Portfolio> findById(Long id) {
        String portfolioSql = """
                SELECT id, user_id, name
                FROM portfolios
                WHERE id = ?
                """;

        Portfolio portfolio;
        try {
            portfolio = jdbcTemplate.queryForObject(portfolioSql, (rs, _) -> {
                Portfolio foundPortfolio = new Portfolio();
                foundPortfolio.setId(rs.getLong("id"));
                foundPortfolio.setUserId(rs.getLong("user_id"));
                foundPortfolio.setName(rs.getString("name"));
                return foundPortfolio;
            }, id);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }

        portfolio.setHoldings(findHoldingsByPortfolioId(id));
        return Optional.of(portfolio);
    }

    @Transactional
    public Portfolio create(Portfolio portfolio) {
        if (portfolio.getId() != null) {
            throw new IllegalArgumentException("id must be null when creating a portfolio");
        }

        Long userId = portfolio.getUserId();
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        Long portfolioId = insertPortfolio(userId, portfolio.getName());
        portfolio.setId(portfolioId);

        List<Holding> holdings = portfolio.getHoldings();
        if (holdings != null) {
            portfolio.setHoldings(replaceHoldings(portfolioId, holdings));
        }

        return portfolio;
    }

    @Transactional
    public Portfolio update(Portfolio portfolio) {
        Long portfolioId = portfolio.getId();
        if (portfolioId == null) {
            throw new IllegalArgumentException("id is required when updating a portfolio");
        }

        Long userId = portfolio.getUserId();
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }

        int updated = jdbcTemplate.update(
                "UPDATE portfolios SET user_id = ?, name = ? WHERE id = ?",
                userId,
                portfolio.getName(),
                portfolioId
        );
        if (updated == 0) {
            throw new IllegalArgumentException("Portfolio not found for id: " + portfolioId);
        }

        List<Holding> holdings = portfolio.getHoldings();
        if (holdings != null) {
            portfolio.setHoldings(replaceHoldings(portfolioId, holdings));
        }

        return portfolio;
    }

    @Transactional
    public void deletePortfolio(Long portfolioId) throws DataAccessException {
        // Delete children first to satisfy FK constraints without issuing a pre-read.
        jdbcTemplate.update("DELETE FROM holdings WHERE portfolio_id = ?", portfolioId);
        jdbcTemplate.update("DELETE FROM portfolios WHERE id = ?", portfolioId);
    }

    private Long insertPortfolio(Long userId, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO portfolios(user_id, name) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setString(2, name);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to generate portfolio id");
        }

        return key.longValue();
    }

    private List<Holding> findHoldingsByPortfolioId(Long portfolioId) {
        String holdingsSql = """
                SELECT h.id,
                       fi.symbol,
                       h.quantity,
                       lp.price AS latest_price
                FROM holdings h
                INNER JOIN financial_instruments fi
                    ON fi.id = h.financial_instrument_id
                LEFT JOIN (
                    SELECT sp.financial_instrument_id,
                           sp.price,
                           ROW_NUMBER() OVER (
                               PARTITION BY sp.financial_instrument_id
                               ORDER BY sp.timestamp DESC, sp.id DESC
                           ) AS row_num
                    FROM stock_prices sp
                ) lp
                    ON lp.financial_instrument_id = h.financial_instrument_id
                   AND lp.row_num = 1
                WHERE h.portfolio_id = ?
                ORDER BY h.id
                """;

        return jdbcTemplate.query(holdingsSql, (rs, _) -> {
            Holding holding = new Holding();
            holding.setId(rs.getLong("id"));
            holding.setSymbol(rs.getString("symbol"));
            holding.setQuantity(rs.getBigDecimal("quantity"));
            holding.setLatestPrice(rs.getBigDecimal("latest_price"));
            holding.setPortfolioId(portfolioId);
            return holding;
        }, portfolioId);
    }

    private List<Holding> replaceHoldings(Long portfolioId, List<Holding> holdings) {
        try {
            if (holdings.isEmpty()) {
                jdbcTemplate.update("DELETE FROM holdings WHERE portfolio_id = ?", portfolioId);
                return new ArrayList<>();
            }

            Map<String, BigDecimal> requestedBySymbol = new LinkedHashMap<>();
            for (Holding holding : holdings) {
                String symbol = holding.getSymbol();
                if (symbol == null || symbol.isBlank()) {
                    throw new IllegalArgumentException("Holding symbol is required");
                }

                BigDecimal quantity = holding.getQuantity() != null ? holding.getQuantity() : BigDecimal.ZERO;
                BigDecimal existingQuantity = requestedBySymbol.putIfAbsent(symbol, quantity);
                if (existingQuantity != null) {
                    throw new IllegalArgumentException("Duplicate holding symbol: " + symbol);
                }
            }

            List<Holding> existingHoldings = findHoldingsByPortfolioId(portfolioId);
            Map<String, Holding> existingBySymbol = new LinkedHashMap<>();
            for (Holding existingHolding : existingHoldings) {
                existingBySymbol.put(existingHolding.getSymbol(), existingHolding);
            }
            Set<String> requestedSymbols = new LinkedHashSet<>(requestedBySymbol.keySet());

            List<Object[]> updateArgs = new ArrayList<>();
            List<Object[]> insertArgs = new ArrayList<>();
            List<String> insertedSymbols = new ArrayList<>();
            List<Object[]> deleteArgs = new ArrayList<>();

            String updateSql = """
                    UPDATE holdings
                    SET quantity = ?
                    WHERE id = ?
                      AND portfolio_id = ?
                    """;

            String insertSql = """
                    INSERT INTO holdings(portfolio_id, financial_instrument_id, quantity)
                    SELECT ?, fi.id, ?
                    FROM financial_instruments fi
                    WHERE fi.symbol = ?
                    """;

            String deleteSql = """
                    DELETE FROM holdings
                    WHERE id = ?
                      AND portfolio_id = ?
                    """;

            for (Map.Entry<String, BigDecimal> requestedEntry : requestedBySymbol.entrySet()) {
                String symbol = requestedEntry.getKey();
                BigDecimal quantity = requestedEntry.getValue();
                Holding existingHolding = existingBySymbol.get(symbol);
                if (existingHolding != null) {
                    updateArgs.add(new Object[]{quantity, existingHolding.getId(), portfolioId});
                    continue;
                }

                insertArgs.add(new Object[]{portfolioId, quantity, symbol});
                insertedSymbols.add(symbol);
            }

            if (!updateArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(updateSql, updateArgs);
            }

            if (!insertArgs.isEmpty()) {
                int[] insertedRows = jdbcTemplate.batchUpdate(insertSql, insertArgs);
                for (int i = 0; i < insertedRows.length; i++) {
                    if (insertedRows[i] == 0) {
                        throw new IllegalArgumentException("Unknown symbol for holding: " + insertedSymbols.get(i));
                    }
                }
            }

            for (Holding existingHolding : existingHoldings) {
                if (requestedSymbols.contains(existingHolding.getSymbol())) {
                    continue;
                }
                deleteArgs.add(new Object[]{existingHolding.getId(), portfolioId});
            }

            if (!deleteArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(deleteSql, deleteArgs);
            }

            return findHoldingsByPortfolioId(portfolioId);
        } catch (DataAccessException ex) {
            throw new IllegalStateException("Failed to sync holdings for portfolio id: " + portfolioId, ex);
        }
    }
}
