package com.example.mely.dto.request;

import com.example.mely.model.Enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeRequest {
    String firstName;
    String lastName;
    String email;
    String phone;
    RoleType roleType;
}
