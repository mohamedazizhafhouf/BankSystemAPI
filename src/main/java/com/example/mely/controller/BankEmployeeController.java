package com.example.mely.controller;

import com.example.mely.dto.request.CreateEmployeeRequest;
import com.example.mely.dto.response.EmployeeResponse;
import com.example.mely.service.BankEmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bank/employees")
public class BankEmployeeController {

    private final BankEmployeeService bankEmployeeService;

    public BankEmployeeController(BankEmployeeService bankEmployeeService) {
        this.bankEmployeeService = bankEmployeeService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankEmployeeService.createEmployee(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<EmployeeResponse>> listEmployees() {
        return ResponseEntity.ok(bankEmployeeService.listEmployees());
    }
}
