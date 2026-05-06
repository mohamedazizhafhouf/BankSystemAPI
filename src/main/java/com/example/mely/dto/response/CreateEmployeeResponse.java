package com.example.mely.dto.response;

import com.example.mely.model.Enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class CreateEmployeeResponse {
    UUID employeeId;
    String firstName;
    String lastName;
    String email;
    RoleType role;
    String password;
}
