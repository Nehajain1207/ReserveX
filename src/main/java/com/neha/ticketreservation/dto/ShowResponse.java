package com.neha.ticketreservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
public class ShowResponse {

    private Long id;

    private String movieTitle;

    private LocalDate showDate;

    private LocalTime showTime;

    private Integer screenNumber;

    private Integer totalSeats;
    private Integer availableSeats;
    private BigDecimal ticketPrice;
}