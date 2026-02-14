package com.example.stockmonitor.repository;

import com.example.stockmonitor.model.Holding;
import com.example.stockmonitor.model.Portfolio;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
                SELECT h.id, fi.symbol, h.quantity
                FROM holdings h
                INNER JOIN financial_instruments fi
                    ON fi.id = h.financial_instrument_id
                WHERE h.portfolio_id = ?
                ORDER BY h.id
                """;

        return jdbcTemplate.query(holdingsSql, (rs, _) -> {
            Holding holding = new Holding();
            holding.setId(rs.getLong("id"));
            holding.setSymbol(rs.getString("symbol"));
            holding.setQuantity(rs.getBigDecimal("quantity"));
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

            List<Holding> existingHoldings = findHoldingsByPortfolioId(portfolioId);
            Map<String, Holding> existingBySymbol = existingHoldings.stream()
                    .collect(Collectors.toMap(Holding::getSymbol, holding -> holding));
            Set<String> requestedSymbols = new LinkedHashSet<>();

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

            for (Holding holding : holdings) {
                String symbol = holding.getSymbol();
                if (symbol == null || symbol.isBlank()) {
                    throw new IllegalArgumentException("Holding symbol is required");
                }
                if (!requestedSymbols.add(symbol)) {
                    throw new IllegalArgumentException("Duplicate holding symbol: " + symbol);
                }

                BigDecimal quantity = holding.getQuantity() != null ? holding.getQuantity() : BigDecimal.ZERO;
                Holding existingHolding = existingBySymbol.get(symbol);
                if (existingHolding != null) {
                    int updatedRows = jdbcTemplate.update(updateSql, quantity, existingHolding.getId(), portfolioId);
                    if (updatedRows == 0) {
                        throw new IllegalStateException("Failed to update holding id: " + existingHolding.getId());
                    }
                    continue;
                }

                int insertedRows = jdbcTemplate.update(insertSql, portfolioId, quantity, symbol);
                if (insertedRows == 0) {
                    throw new IllegalArgumentException("Unknown symbol for holding: " + symbol);
                }
            }

            for (Holding existingHolding : existingHoldings) {
                if (!requestedSymbols.contains(existingHolding.getSymbol())) {
                    jdbcTemplate.update("DELETE FROM holdings WHERE id = ? AND portfolio_id = ?",
                            existingHolding.getId(), portfolioId);
                }
            }

            return findHoldingsByPortfolioId(portfolioId);
        } catch (DataAccessException ex) {
            throw new IllegalStateException("Failed to sync holdings for portfolio id: " + portfolioId, ex);
        }
    }
}
