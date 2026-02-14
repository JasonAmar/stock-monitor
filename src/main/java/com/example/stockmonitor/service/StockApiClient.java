package com.example.stockmonitor.service;

import com.example.stockmonitor.model.StockPrice;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class StockApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${stock.api.base-url}")
    private String baseUrl;

    public StockPrice fetch(String symbol) {
        String url = baseUrl + "/" + symbol;
        double price;

        try {
            ResponseEntity<Map<String, Object>> apiResponse = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            if (!apiResponse.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException(
                        "Stock API request returned non-success status for symbol "
                                + symbol + ": " + apiResponse.getStatusCode());
            }
            Map<String, Object> response = apiResponse.getBody();

            if (response == null || response.get("price") == null) {
                throw new IllegalStateException("Stock API response missing price for symbol: " + symbol);
            }
            price = Double.parseDouble(response.get("price").toString());
        } catch (RestClientException ex) {
            throw new IllegalStateException("Stock API request failed for symbol: " + symbol, ex);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Stock API returned invalid price for symbol: " + symbol, ex);
        }
        return new StockPrice(symbol, price, LocalDateTime.now());
    }
}
