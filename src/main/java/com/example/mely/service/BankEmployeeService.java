package com.example.mely.service;

import com.example.mely.dto.request.CreateEmployeeRequest;
import com.example.mely.dto.response.EmployeeResponse;
import com.example.mely.model.Enums.RoleType;
import com.example.mely.model.Membership;
import com.example.mely.model.User;
import com.example.mely.repository.MembershipRepository;
import com.example.mely.repository.UserRepository;
import com.example.mely.security.CurrentOrganizationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class BankEmployeeService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentOrganizationProvider currentOrganizationProvider;

    public BankEmployeeService(UserRepository userRepository,
                               MembershipRepository membershipRepository,
                               PasswordEncoder passwordEncoder,
                               CurrentOrganizationProvider currentOrganizationProvider) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentOrganizationProvider = currentOrganizationProvider;
    }

    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        validateEmployeeRole(request.getRole());

        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .roleType(request.getRole())
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        Membership membership = Membership.builder()
                .user(user)
                .role(request.getRole())
                .build();
        membership.setOrganizationId(currentOrganizationProvider.getCurrentOrganizationId());
        membership.setCreatedAt(LocalDateTime.now());
        membershipRepository.save(membership);

        return toResponse(user, membership.getRole());
    }

    public List<EmployeeResponse> listEmployees() {
        return membershipRepository.findByOrganizationId(currentOrganizationProvider.getCurrentOrganizationId())
                .stream()
                .map(membership -> toResponse(membership.getUser(), membership.getRole()))
                .toList();
    }

    private void validateEmployeeRole(RoleType role) {
        if (role == RoleType.ADMIN) {
            throw new IllegalArgumentException("Employee creation cannot assign the ADMIN role in v1.");
        }
        if (role == RoleType.SAAS_USER) {
            throw new IllegalArgumentException("Bank employee accounts cannot use the SAAS_USER role.");
        }
    }

    private EmployeeResponse toResponse(User user, RoleType role) {
        return EmployeeResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(role.name())
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
