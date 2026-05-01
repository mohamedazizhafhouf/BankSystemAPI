package com.example.mely.dto.request;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//Step 2: otp verification

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequestDTO {
    String email;
    String otp;
    String password;
}
