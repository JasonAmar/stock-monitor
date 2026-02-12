package com.example.stockmonitor.event;

import com.example.stockmonitor.model.StockPrice;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PriceEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public PriceEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishPriceUpdate(StockPrice price) {
        eventPublisher.publishEvent(price);
    }
}
