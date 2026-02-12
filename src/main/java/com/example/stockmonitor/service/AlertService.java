package com.example.stockmonitor.service;

import com.example.stockmonitor.model.Alert;
import com.example.stockmonitor.model.StockPrice;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    public List<Alert> evaluateAlerts(StockPrice stockPrice) {
        return new ArrayList<>();
    }
}
