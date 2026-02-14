package com.example.stockmonitor.service;

import com.example.stockmonitor.model.Portfolio;
import com.example.stockmonitor.repository.PortfolioRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public PortfolioService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    public List<Portfolio> findAll() {
        return portfolioRepository.findAll();
    }

    public Optional<Portfolio> findById(Long id) {
        return portfolioRepository.findById(id);
    }

    public Portfolio create(Portfolio portfolio) {
        return portfolioRepository.create(portfolio);
    }

    public Portfolio update(Portfolio portfolio) {
        return portfolioRepository.update(portfolio);
    }

    public void delete(Long id) {
        portfolioRepository.deletePortfolio(id);
    }
}
