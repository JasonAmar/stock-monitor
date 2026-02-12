package com.example.stockmonitor.controller;

import com.example.stockmonitor.model.StockPrice;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
@RequestMapping("/api/stream")
public class PriceStreamController {

    private final Sinks.Many<StockPrice> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void publish(StockPrice price) {
        sink.tryEmitNext(price);
    }

    @GetMapping(value = "/prices",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StockPrice> streamPrices() {
        return sink.asFlux();
    }
}
