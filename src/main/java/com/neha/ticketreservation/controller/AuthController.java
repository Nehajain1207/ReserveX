package com.neha.ticketreservation.controller;

import com.neha.ticketreservation.dto.LoginRequest;
import com.neha.ticketreservation.dto.LoginResponse;
import com.neha.ticketreservation.dto.RegisterRequest;
import com.neha.ticketreservation.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public String register(
            @Valid @RequestBody RegisterRequest request) {

        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request);
    }
}