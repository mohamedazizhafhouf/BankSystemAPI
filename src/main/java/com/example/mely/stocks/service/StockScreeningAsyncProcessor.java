package com.example.mely.stocks.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StockScreeningAsyncProcessor {

    private final StockScreeningProcessingService processingService;

    public StockScreeningAsyncProcessor(StockScreeningProcessingService processingService) {
        this.processingService = processingService;
    }

    @Async("stockScreeningExecutor")
    public void process(UUID requestId) {
        processingService.processRequest(requestId);
    }
}
