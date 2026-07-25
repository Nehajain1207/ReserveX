package com.neha.ticketreservation.controller;

import com.neha.ticketreservation.dto.AuditoriumRequest;
import com.neha.ticketreservation.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping("/shows/{showId}/initialize-auditorium")
    @ResponseStatus(HttpStatus.CREATED)
    public String initializeAuditorium(
            @PathVariable Long showId,
            @Valid @RequestBody AuditoriumRequest request) {

        seatService.initializeAuditorium(showId, request);

        return "Auditorium initialized successfully!";
    }
}