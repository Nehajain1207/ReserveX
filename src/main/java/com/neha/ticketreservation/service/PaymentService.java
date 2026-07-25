package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.PaymentResponse;

public interface PaymentService {

    PaymentResponse confirmPayment(
            String bookingReference,
            String userEmail,
            String idempotencyKey
    );

}