package com.example.mely.personal.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.mely.dto.request.ZakatAssetRequest;
import com.example.mely.dto.response.ZakatCalculationResponse;
import com.example.mely.model.User;
import com.example.mely.model.ZakatAsset;
import com.example.mely.personal.dto.HawlStatusResponse;
import com.example.mely.personal.dto.ZakatAssetApiResponse;
import com.example.mely.repository.UserRepository;
import com.example.mely.security.AuthenticatedUser;
import com.example.mely.security.CurrentAuthenticatedUserProvider;
import com.example.mely.service.ZakatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/personal/zakat-portfolio")
@RequiredArgsConstructor
public class PersonalZakatPortfolioController {

    private final CurrentAuthenticatedUserProvider currentUser;
    private final ZakatService zakatService;
    private final UserRepository userRepository;

    @GetMapping("/assets")
    public List<ZakatAssetApiResponse> listAssets() {
        AuthenticatedUser u = requireSaas();
        return zakatService.listActiveZakatAssets(u.getUserId()).stream()
                .map(ZakatAssetApiResponse::from)
                .toList();
    }

    @PostMapping("/assets")
    public ZakatAssetApiResponse addAsset(@Valid @RequestBody ZakatAssetRequest request) {
        AuthenticatedUser u = requireSaas();
        ZakatAsset saved = zakatService.addZakatAssetForUser(u.getUserId(), request);
        return ZakatAssetApiResponse.from(saved);
    }

    @PutMapping("/assets/{assetId}")
    public ZakatAssetApiResponse updateAsset(
            @PathVariable UUID assetId,
            @Valid @RequestBody ZakatAssetRequest request) {
        AuthenticatedUser u = requireSaas();
        return ZakatAssetApiResponse.from(zakatService.updateZakatAssetForUser(u.getUserId(), assetId, request));
    }

    @DeleteMapping("/assets/{assetId}")
    public ResponseEntity<Void> deactivateAsset(@PathVariable UUID assetId) {
        AuthenticatedUser u = requireSaas();
        zakatService.deactivateZakatAssetForUser(u.getUserId(), assetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/hawl")
    public ResponseEntity<HawlStatusResponse> hawl() {
        AuthenticatedUser u = requireSaas();
        var opt = zakatService.findHawlTracking(u.getUserId());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.ok(HawlStatusResponse.from(opt.get()));
    }

    @PostMapping("/calculate")
    public ZakatCalculationResponse calculate() {
        AuthenticatedUser u = requireSaas();
        return zakatService.calculateZakat(u.getUserId());
    }

    @PostMapping("/enable-tracking")
    public ResponseEntity<Void> enableTracking() {
        AuthenticatedUser u = requireSaas();
        User user = userRepository.findById(u.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.updateZakatHandling(true, zakatService);
        userRepository.save(user);
        return ResponseEntity.accepted().build();
    }

    private AuthenticatedUser requireSaas() {
        AuthenticatedUser u = currentUser.getCurrentUser();
        if (!"SAAS".equals(u.getAccountType())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Wealth hub is available for individual (SAAS) accounts.");
        }
        return u;
    }
}
