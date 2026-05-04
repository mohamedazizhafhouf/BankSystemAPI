package com.example.mely.controller;

import com.example.mely.dto.request.CreateEmployeeRequest;
import com.example.mely.dto.response.CreateEmployeeResponse;
import com.example.mely.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/employees")
public class EmployeesController {
    private final EmployeeService employeeService;

    public EmployeesController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreateEmployeeResponse> createEmployee(@RequestBody CreateEmployeeRequest request, Authentication authentication) {
        CreateEmployeeResponse response = employeeService.createEmployee(request, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
