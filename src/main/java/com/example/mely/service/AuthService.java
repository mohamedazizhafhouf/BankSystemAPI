package com.example.mely.service;

import com.example.mely.model.Membership;
import com.example.mely.model.User;
import com.example.mely.repository.MembershipRepository;
import com.example.mely.repository.UserRepository;
import com.example.mely.security.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepo;
    @Autowired private MembershipRepository membershipRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    public String authenticateUser(String email, String password) {
        // 1 - Find User
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2 - Check Password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        Membership membership = membershipRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("User has no organization assigned"));

        // 3 - Generate token
        return jwtUtils.generateJwtToken(
                user.getEmail(),
                membership.getRole().name(), // This works now because RoleType is an Enum
                membership.getOrganizationId()
        );
    }}
