package com.example.mely.zakat.controller;

import com.example.mely.zakat.dto.ZakatCalculationRequest;
import com.example.mely.zakat.dto.ZakatCalculationResponse;
import com.example.mely.zakat.dto.ZakatDefaultsResponse;
import com.example.mely.zakat.service.ZakatCalculatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/zakat")
public class ZakatController {

    private final ZakatCalculatorService zakatCalculatorService;

    public ZakatController(ZakatCalculatorService zakatCalculatorService) {
        this.zakatCalculatorService = zakatCalculatorService;
    }

    @GetMapping("/defaults")
    public ResponseEntity<ZakatDefaultsResponse> defaults() {
        return ResponseEntity.ok(zakatCalculatorService.getDefaults());
    }

    @PostMapping("/calculate")
    public ResponseEntity<ZakatCalculationResponse> calculate(@Valid @RequestBody ZakatCalculationRequest request) {
        return ResponseEntity.ok(zakatCalculatorService.calculate(request));
    }
}
