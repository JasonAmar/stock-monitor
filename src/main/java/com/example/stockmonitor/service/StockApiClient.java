package com.example.stockmonitor.service;

import com.example.stockmonitor.model.StockPrice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class StockApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${stock.api.base-url}")
    private String baseUrl;

    public StockPrice fetch(String symbol) {
        Map<String, Object> response =
                restTemplate.getForObject(baseUrl + "/" + symbol, Map.class);

        Double price = Double.valueOf(response.get("price").toString());

        return new StockPrice(symbol, price, LocalDateTime.now());
    }
}

