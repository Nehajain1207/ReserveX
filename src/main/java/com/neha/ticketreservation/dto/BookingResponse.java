package com.neha.ticketreservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BookingResponse {

    private String bookingReference;

    private String status;

    private Integer seatsBooked;
}