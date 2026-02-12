package com.example.stockmonitor.controller;

import com.example.stockmonitor.model.Portfolio;
import com.example.stockmonitor.model.dtos.PortfolioDTO;
import com.example.stockmonitor.service.PortfolioService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public List<PortfolioDTO> getAll() {
        return portfolioService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioDTO> getById(@PathVariable Long id) {
        return portfolioService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public PortfolioDTO create(@RequestBody Portfolio portfolio) {
        return portfolioService.save(portfolio);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<PortfolioDTO> delete(@PathVariable Long id) {
        portfolioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
