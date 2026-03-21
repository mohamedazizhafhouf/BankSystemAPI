package com.example.mely.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String message;
    private boolean success;
    private LocalDateTime timeStamp;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
