package com.neha.ticketreservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PaymentRequest {

    private String bookingReference;

    private String paymentStatus;

    private String message;
}