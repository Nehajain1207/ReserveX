package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.ShowRequest;
import com.neha.ticketreservation.dto.ShowResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ShowService {

    ShowResponse createShow(ShowRequest request);

    ShowResponse getShowById(Long id);

    Page<ShowResponse> getAllShows(Pageable pageable);

    Page<ShowResponse> searchShows(
            Long movieId,
            LocalDate showDate,
            Pageable pageable);

    ShowResponse updateShow(Long id, ShowRequest request);

    void deleteShow(Long id);
}