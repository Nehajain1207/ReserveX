package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.LoginRequest;
import com.neha.ticketreservation.dto.LoginResponse;
import com.neha.ticketreservation.dto.RegisterRequest;

public interface AuthService {

    String register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

}