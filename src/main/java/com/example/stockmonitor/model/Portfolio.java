package com.example.stockmonitor.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Portfolio {
    private Long id;
    private String name;
    private List<Holding> holdings;
}
