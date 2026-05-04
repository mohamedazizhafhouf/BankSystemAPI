package com.example.mely.security;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuthenticatedUser {
    private final UUID userId;
    private final String email;
    private final String role;
    private final UUID organizationId;
    private final String accountType;

    public boolean hasOrganizationContext() {
        return organizationId != null;
    }
}
