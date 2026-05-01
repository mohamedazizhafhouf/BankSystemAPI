package com.example.mely.service;

import com.example.mely.model.Enums.RoleType;
import com.example.mely.model.Membership;
import com.example.mely.model.Organization;
import com.example.mely.model.User;
import com.example.mely.repository.MembershipRepository;
import com.example.mely.repository.OrganizationRepository;
import com.example.mely.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Transactional
public class ProvisioningService {
    @Autowired
    private OrganizationRepository orgRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private MembershipRepository membershipRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    public void provisionNewBank(String bankName, String adminEmail, String tempPassword, String firstName, String lastName) {
        // 1 - Create the Organization (The Tenant)
        Organization org = new Organization();
        org.setName(bankName);
        org.setSlug(bankName.toLowerCase().replace(" ", "-"));
        org = orgRepo.save(org);

        // 2 - Create the User
        User adminUser = new User();
        adminUser.setFirstName(firstName);
        adminUser.setLastName(lastName);
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode(tempPassword));
        adminUser.setRoleType(RoleType.ADMIN);
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser = userRepo.save(adminUser);

        // 3 - Link User to Org as ADMIN
        Membership membership = new Membership();
        membership.setUser(adminUser);
        membership.setOrganizationId(org.getId()); // Set the Tenant ID
        membership.setRole(RoleType.ADMIN);
        membership.setCreatedAt(LocalDateTime.now());
        membershipRepo.save(membership);
    }
}
