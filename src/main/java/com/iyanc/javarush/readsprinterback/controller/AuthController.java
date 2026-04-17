package com.iyanc.javarush.readsprinterback.controller;

import com.iyanc.javarush.readsprinterback.dto.request.LoginRequest;
import com.iyanc.javarush.readsprinterback.dto.request.RefreshTokenRequest;
import com.iyanc.javarush.readsprinterback.dto.request.RegisterRequest;
import com.iyanc.javarush.readsprinterback.dto.response.AuthResponse;
import com.iyanc.javarush.readsprinterback.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login and token refresh")
public class AuthController {

    private final AuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user (sends verification email)")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful. Please check your email to verify your account."));
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify email address using token from email link")
    public ResponseEntity<Void> verify(@RequestParam String token) {
        authService.verifyEmail(token);
        URI redirectUri = URI.create(frontendUrl + "/auth?verified=true");
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUri.toString())
                .build();
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user info")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserDetails> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok((UserDetails) authentication.getPrincipal());
    }
}
