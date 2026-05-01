package com.example.mely.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String taskType; // e.g: "Murabaha_underwriting" or "Zakat_Calculation"

    @JdbcTypeCode(SqlTypes.JSON)
    private String inputData; // The uploaded PDF data or numbers

    @JdbcTypeCode(SqlTypes.JSON)
    private String aiAnalysis; // The AI's reasoning, confidence score, and Sharia verdict

    private String status; // PENDING, APPROVED, REJECTED
}