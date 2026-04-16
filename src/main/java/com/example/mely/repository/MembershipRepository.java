package com.example.mely.repository;

import com.example.mely.model.Membership;
import com.example.mely.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByUser(User user);}
