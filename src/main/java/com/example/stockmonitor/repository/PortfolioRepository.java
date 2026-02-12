package com.example.stockmonitor.repository;

import com.example.stockmonitor.model.Holding;
import com.example.stockmonitor.model.Portfolio;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
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
                SELECT id, name
                FROM portfolios
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, (rs, _) -> {
            Portfolio portfolio = new Portfolio();
            portfolio.setId(rs.getLong("id"));
            portfolio.setName(rs.getString("name"));
            portfolio.setHoldings(new ArrayList<>());
            return portfolio;
        });
    }

    public Optional<Portfolio> findById(Long id) {
        String portfolioSql = """
                SELECT id, name
                FROM portfolios
                WHERE id = ?
                """;

        List<Portfolio> portfolios = jdbcTemplate.query(portfolioSql, (rs, rowNum) -> {
            Portfolio portfolio = new Portfolio();
            portfolio.setId(rs.getLong("id"));
            portfolio.setName(rs.getString("name"));
            return portfolio;
        }, id);

        if (portfolios.isEmpty()) {
            return Optional.empty();
        }

        Portfolio portfolio = portfolios.getFirst();
        portfolio.setHoldings(findHoldingsByPortfolioId(id));
        return Optional.of(portfolio);
    }

    @Transactional
    public Portfolio save(Portfolio portfolio) {
        Long portfolioId = portfolio.getId();
        if (portfolioId == null) {
            portfolioId = insertPortfolio(portfolio.getName());
            portfolio.setId(portfolioId);
        } else {
            int updated = jdbcTemplate.update(
                    "UPDATE portfolios SET name = ? WHERE id = ?",
                    portfolio.getName(),
                    portfolioId
            );
            if (updated == 0) {
                throw new IllegalArgumentException("Portfolio not found for id: " + portfolioId);
            }
        }

        List<Holding> holdings = portfolio.getHoldings();
        if (holdings != null) {
            replaceHoldings(portfolioId, holdings);
        }

        return portfolio;
    }

    @Transactional
    public void deletePortfolio(Long portfolioId) throws DataAccessException {
        // Delete children first to satisfy FK constraints without issuing a pre-read.
        jdbcTemplate.update("DELETE FROM holdings WHERE portfolio_id = ?", portfolioId);
        jdbcTemplate.update("DELETE FROM portfolios WHERE id = ?", portfolioId);
    }

    private Long insertPortfolio(String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO portfolios(name) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name);
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
                SELECT id, symbol, quantity
                FROM holdings
                WHERE portfolio_id = ?
                ORDER BY id
                """;

        return jdbcTemplate.query(holdingsSql, (rs, rowNum) -> {
            Holding holding = new Holding();
            holding.setId(rs.getLong("id"));
            holding.setSymbol(rs.getString("symbol"));
            holding.setQuantity(rs.getBigDecimal("quantity"));
            return holding;
        }, portfolioId);
    }

    private void replaceHoldings(Long portfolioId, List<Holding> holdings) {
        jdbcTemplate.update("DELETE FROM holdings WHERE portfolio_id = ?", portfolioId);
        if (holdings.isEmpty()) {
            return;
        }

        String insertSql = """
                INSERT INTO holdings(portfolio_id, symbol, quantity)
                VALUES (?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(insertSql, holdings, holdings.size(), (ps, holding) -> {
            ps.setLong(1, portfolioId);
            ps.setString(2, holding.getSymbol());
            if (holding.getQuantity() != null) {
                ps.setBigDecimal(3, holding.getQuantity());
            } else {
                ps.setNull(3, Types.DOUBLE);
            }
        });
    }
}
