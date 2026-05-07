package com.example.mely.metals.controller;

import com.example.mely.metals.dto.MetalHistoryChartResponse;
import com.example.mely.metals.dto.MetalsSpotBundleResponse;
import com.example.mely.metals.service.GoldApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/metals")
public class MetalsController {

    private final GoldApiService goldApiService;

    public MetalsController(GoldApiService goldApiService) {
        this.goldApiService = goldApiService;
    }

    @GetMapping("/spot")
    public ResponseEntity<MetalsSpotBundleResponse> spot(
            @RequestParam(name = "currency", defaultValue = "USD") String currency
    ) {
        return ResponseEntity.ok(goldApiService.fetchSpotBundle(currency));
    }

    @GetMapping("/history-chart")
    public ResponseEntity<MetalHistoryChartResponse> historyChart(
            @RequestParam(name = "currency", defaultValue = "USD") String currency,
            @RequestParam(name = "days", defaultValue = "10") int days
    ) {
        return ResponseEntity.ok(goldApiService.fetchHistoryChart(currency, days));
    }
}
