package com.example.mely.model;

import com.example.mely.model.Enums.AccountStatus;
import com.example.mely.model.Enums.ContractType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    private ContractType contractType;

    @Column(length = 3)
    private String currencyCode;

    @Column(precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(precision = 5, scale = 2)
    private BigDecimal profitShareRatio;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    private LocalDate openingDate;
}