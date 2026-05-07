package com.example.mely.stocks.controller;

import com.example.mely.stocks.dto.StockExplanationResponse;
import com.example.mely.stocks.dto.StockScreeningHistoryResponse;
import com.example.mely.stocks.dto.StockScreeningRequestForm;
import com.example.mely.stocks.dto.StockScreeningResponse;
import com.example.mely.stocks.model.StockScreeningStatus;
import com.example.mely.stocks.service.StockScreeningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stocks/check")
public class StockScreeningController {

    private final StockScreeningService stockScreeningService;

    public StockScreeningController(StockScreeningService stockScreeningService) {
        this.stockScreeningService = stockScreeningService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN','LOAN_OFFICER','SHARIA_AUDITOR','SAAS_USER')")
    public ResponseEntity<StockScreeningResponse> submit(@RequestBody StockScreeningRequestForm request) {
        StockScreeningResponse response = stockScreeningService.submit(request);
        HttpStatus status = response.getStatus() == StockScreeningStatus.COMPLETED ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyAuthority('ADMIN','LOAN_OFFICER','SHARIA_AUDITOR','SAAS_USER')")
    public ResponseEntity<StockScreeningHistoryResponse> getHistory(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size,
                                                                    @RequestParam(required = false) String status,
                                                                    @RequestParam(required = false) String verdict,
                                                                    @RequestParam(required = false) String query) {
        return ResponseEntity.ok(stockScreeningService.getHistory(page, size, status, verdict, query));
    }

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','LOAN_OFFICER','SHARIA_AUDITOR','SAAS_USER')")
    public ResponseEntity<StockScreeningResponse> getById(@PathVariable UUID requestId) {
        return ResponseEntity.ok(stockScreeningService.getById(requestId));
    }

    @GetMapping("/{requestId}/explanation")
    @PreAuthorize("hasAnyAuthority('ADMIN','LOAN_OFFICER','SHARIA_AUDITOR','SAAS_USER')")
    public ResponseEntity<StockExplanationResponse> getExplanation(@PathVariable UUID requestId) {
        return ResponseEntity.ok(stockScreeningService.getExplanation(requestId));
    }
}
