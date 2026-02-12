package com.example.stockmonitor.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
public class Alert {
    private Long id;
    private String symbol;
    private BigDecimal threshold;
    private String direction;
    private boolean triggered;
    private Instant createdAt;
}
