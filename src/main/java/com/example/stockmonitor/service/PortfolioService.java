package com.example.stockmonitor.service;

import com.example.stockmonitor.model.Portfolio;
import com.example.stockmonitor.model.dtos.PortfolioDTO;
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

    public List<PortfolioDTO> findAll() {
        return portfolioRepository.findAll().stream()
                .map(PortfolioDTO::new).toList();
    }

    public Optional<PortfolioDTO> findById(Long id) {
        return portfolioRepository.findById(id)
                .map(PortfolioDTO::new);
    }

    public PortfolioDTO save(Portfolio portfolio) {
        return new PortfolioDTO(portfolioRepository.save(portfolio));
    }

    public void delete(Long id) {
        portfolioRepository.deletePortfolio(id);
    }
}
