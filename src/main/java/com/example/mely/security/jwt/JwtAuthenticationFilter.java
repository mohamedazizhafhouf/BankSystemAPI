package com.example.mely.security.jwt;

import com.example.mely.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtils.isTokenValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                Claims claims = jwtUtils.extractClaims(token);
                String role = claims.get("role", String.class);
                String orgId = claims.get("orgId", String.class);
                String accountType = claims.get("accountType", String.class);
                String userId = claims.get("userId", String.class);

                AuthenticatedUser user = AuthenticatedUser.builder()
                        .userId(UUID.fromString(userId))
                        .email(claims.getSubject())
                        .role(role)
                        .organizationId(orgId != null ? UUID.fromString(orgId) : null)
                        .accountType(accountType)
                        .build();

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
