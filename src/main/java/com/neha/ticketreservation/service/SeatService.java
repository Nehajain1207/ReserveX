package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.AuditoriumRequest;
import java.util.List;

public interface SeatService {

    void initializeAuditorium(Long showId, AuditoriumRequest request);

    List<String> getAvailableSeats(Long showId);
}