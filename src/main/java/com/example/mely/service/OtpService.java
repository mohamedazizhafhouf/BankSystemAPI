package com.example.mely.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    private final Map<String, String> otpCache = new ConcurrentHashMap<>();

    public String generateOtp(String email) {
        String code = String.format("%06d", new SecureRandom().nextInt(999999));
        otpCache.put(email, code);
        return code;
    }

    public boolean validateOtp(String email, String code) {
        return code.equals(otpCache.get(email));
    }
}
