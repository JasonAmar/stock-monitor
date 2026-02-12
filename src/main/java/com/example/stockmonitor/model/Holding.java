package com.example.stockmonitor.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class Holding {
    private Long id;
    private String symbol;
    private BigDecimal quantity;
    private Portfolio portfolio;
}
