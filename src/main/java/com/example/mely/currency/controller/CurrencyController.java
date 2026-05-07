package com.example.mely.currency.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.mely.currency.CurrencylayerProperties;
import com.example.mely.currency.dto.CurrencyConvertResponse;
import com.example.mely.currency.service.CurrencylayerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencylayerProperties properties;
    private final CurrencylayerService currencylayerService;

    @GetMapping("/convert")
    public ResponseEntity<?> convert(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam("amount") BigDecimal amount) {
        if (!properties.hasApiKey()) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(Map.of(
                            "message",
                            "Currency conversion is not configured. Set mely.currencylayer.api-key or CURRENCYLAYER_API_KEY."));
        }
        CurrencyConvertResponse body = currencylayerService.convert(from, to, amount);
        return ResponseEntity.ok(body);
    }
}
