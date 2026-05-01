package com.example.mely.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organization")
public class Organization {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name; // e.g: "Baraka Bank"
    private String slug; // e.g: "baraka-bank" for subdomains

    // AI specific behavior configuration
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> agentConfig;
}
