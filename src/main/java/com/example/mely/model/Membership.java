package com.example.mely.model;

import com.example.mely.model.Enums.RoleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "memberships")
public class Membership extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    private User user;

    @Enumerated(EnumType.STRING)
    private RoleType role;
}