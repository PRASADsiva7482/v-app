package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
@Tag(name = "Verification", description = "Verified Checkmark Application APIs")
public class VerificationController {

    private final VerificationService verificationService;

    @Operation(summary = "Submit a verification request")
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> submitRequest(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String userId = authentication.getName();
        return ResponseEntity.ok(verificationService.submitRequest(
                userId, body.get("fullName"), body.get("category"), body.get("reason")));
    }

    @Operation(summary = "Get my verification requests")
    @GetMapping("/requests")
    public ResponseEntity<List<Map<String, Object>>> getMyRequests(Authentication authentication) {
        return ResponseEntity.ok(verificationService.getMyRequests(authentication.getName()));
    }

    @Operation(summary = "Get latest verification status")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(Authentication authentication) {
        return ResponseEntity.ok(verificationService.getLatestRequest(authentication.getName()));
    }
}
