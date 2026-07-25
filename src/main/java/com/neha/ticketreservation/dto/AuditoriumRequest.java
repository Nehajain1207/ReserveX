package com.neha.ticketreservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditoriumRequest {

    @NotNull
    @Min(1)
    private Integer rows;

    @NotNull
    @Min(1)
    private Integer seatsPerRow;
}