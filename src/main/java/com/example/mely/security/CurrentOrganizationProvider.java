package com.example.mely.security;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentOrganizationProvider {

    private final CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider;

    public CurrentOrganizationProvider(CurrentAuthenticatedUserProvider currentAuthenticatedUserProvider) {
        this.currentAuthenticatedUserProvider = currentAuthenticatedUserProvider;
    }

    public UUID getCurrentOrganizationId() {
        AuthenticatedUser user = currentAuthenticatedUserProvider.getCurrentUser();
        if (user.getOrganizationId() == null) {
            throw new IllegalStateException("Authenticated organization context is missing");
        }
        return user.getOrganizationId();
    }
}
