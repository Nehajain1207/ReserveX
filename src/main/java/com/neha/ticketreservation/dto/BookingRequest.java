package com.neha.ticketreservation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookingRequest {

    @NotNull
    private Long showId;

    @NotEmpty
    private List<String> seatNumbers;
}