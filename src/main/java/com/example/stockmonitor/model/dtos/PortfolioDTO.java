package com.example.stockmonitor.model.dtos;

import com.example.stockmonitor.model.Holding;
import com.example.stockmonitor.model.Portfolio;

import java.util.List;

public record PortfolioDTO (Long id, String name, List<Holding> holdings) {
    public PortfolioDTO (Portfolio portfolio) {
        this(portfolio.getId(), portfolio.getName(), portfolio.getHoldings());
    }
}
