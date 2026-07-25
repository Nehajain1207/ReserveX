package com.neha.ticketreservation.controller;

import com.neha.ticketreservation.service.LogoutService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class LogoutController {

    private final LogoutService logoutService;

    public LogoutController(LogoutService logoutService) {
        this.logoutService = logoutService;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public String logout(
            @RequestHeader("Authorization") String authHeader) {

        logoutService.logout(authHeader);

        return "Logged out successfully.";
    }
}