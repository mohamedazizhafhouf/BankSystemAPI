package com.example.mely.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.mely.model.Enums.AssetType;
import com.example.mely.model.ZakatAsset;
@Repository
public interface ZakatAssetRepository extends JpaRepository<ZakatAsset, UUID> {

     List<ZakatAsset> findByUserIdAndIsActiveTrue(UUID userId);
    
    List<ZakatAsset> findByUserIdAndAssetType(UUID userId, AssetType assetType);
    
    @Query("SELECT SUM(z.value) FROM ZakatAsset z WHERE z.userId = :userId AND z.isActive = true AND z.acquisitionDate <= :cutoffDate")
    BigDecimal calculateTotalAssetValue(@Param("userId") UUID userId, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT z.assetType, SUM(z.value) FROM ZakatAsset z WHERE z.userId = :userId AND z.isActive = true GROUP BY z.assetType")
    List<Object[]> getAssetBreakdownByType(@Param("userId") UUID userId);



}
