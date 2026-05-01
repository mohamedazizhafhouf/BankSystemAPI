package com.example.mely.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@Data
@Getter
@Setter
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "orgId", type = java.util.UUID.class))
@Filter(name = "tenantFilter", condition = "organization_id = :orgId")
public abstract class BaseEntity {
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


}
