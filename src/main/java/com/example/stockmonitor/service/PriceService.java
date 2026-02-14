package com.example.stockmonitor.service;

import com.example.stockmonitor.model.StockPrice;
import com.example.stockmonitor.repository.PriceRepository;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PriceService {

    private final StockApiClient stockApiClient;
    private final PriceRepository priceRepository;

    public PriceService(StockApiClient stockApiClient,
                        PriceRepository priceRepository) {
        this.stockApiClient = stockApiClient;
        this.priceRepository = priceRepository;
    }

    @Async("stockExecutor")
    @Scheduled(fixedRate = 60000)
    public void fetchPrices() {

        List<String> symbols = List.of("AAPL", "TSLA", "MSFT");

        try {
            symbols.parallelStream().forEach(symbol -> {
                StockPrice price = stockApiClient.fetch(symbol);
                priceRepository.save(price);
            });
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
