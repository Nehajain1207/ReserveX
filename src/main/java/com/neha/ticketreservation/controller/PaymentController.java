package com.neha.ticketreservation.controller;

import com.neha.ticketreservation.dto.PaymentResponse;
import com.neha.ticketreservation.service.PaymentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{bookingReference}")
    public PaymentResponse confirmPayment(
            @PathVariable String bookingReference,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {

        return paymentService.confirmPayment(
                bookingReference,
                authentication.getName(),
                idempotencyKey
        );
    }
}