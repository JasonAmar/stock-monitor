package com.example.stockmonitor.controller;

import com.example.stockmonitor.model.Portfolio;
import com.example.stockmonitor.service.PortfolioService;
import java.util.List;
import org.springframework.http.HttpStatus;
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
    public List<Portfolio> getAll() {
        return portfolioService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Portfolio> getById(@PathVariable Long id) {
        return portfolioService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Portfolio> create(@RequestBody Portfolio portfolio) {
        portfolio.setId(null);
        try {
            Portfolio created = portfolioService.create(portfolio);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Portfolio> update(@PathVariable Long id,
                                               @RequestBody Portfolio portfolio) {
        if (portfolioService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        portfolio.setId(id);
        try {
            Portfolio updated = portfolioService.update(portfolio);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (portfolioService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        portfolioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
