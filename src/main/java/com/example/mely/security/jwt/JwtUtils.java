package com.example.mely.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtils {

    @Value("${mely.jwt.secret}")
    private String jwtSecret;

    @Value("${mely.jwt.expiration}")
    private int jwtExpirationMs;

    public String generateJwtToken(String email, String role, UUID orgId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("orgId", orgId.toString()) // This is the multi-tenant magic
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }
}