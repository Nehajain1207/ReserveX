package com.neha.ticketreservation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ShowRequest {

    @NotNull(message = "Movie Id is required")
    private Long movieId;

    @NotNull(message = "Show Date is required")
    private LocalDate showDate;

    @NotNull(message = "Show Time is required")
    private LocalTime showTime;

    @NotNull(message = "Screen Number is required")
    private Integer screenNumber;

    @NotNull(message = "Ticket price is required")
    private BigDecimal ticketPrice;
}