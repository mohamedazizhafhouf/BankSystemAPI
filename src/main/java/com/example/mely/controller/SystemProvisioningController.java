package com.example.mely.controller;
import com.example.mely.dto.request.ProvisionBankRequest;
import com.example.mely.service.ProvisioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/provision")
public class SystemProvisioningController {
    @Autowired
    private ProvisioningService provisioningService;

    // A secret key stored in your env variables for security
    @Value("${mely.system.provision-key}")
    private String systemProvisionKey;

    @PostMapping
    public ResponseEntity<String> provisionBank(
            @RequestHeader("X-Mely-System-Key") String providedKey,
            @RequestBody ProvisionBankRequest request) {

        // Simple but effective gate for your prototype
        if (!systemProvisionKey.equals(providedKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid System Key");
        }

        provisioningService.provisionNewBank(
                request.getFirstName(),
                request.getLastName(),
                request.getBankName(),
                request.getAdminEmail(),
                request.getTempPassword()
        );

        return ResponseEntity.ok("Bank Workspace '" + request.getBankName() + "' created successfully.");
    }
}
