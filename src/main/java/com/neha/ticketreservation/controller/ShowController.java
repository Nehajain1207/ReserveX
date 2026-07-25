package com.neha.ticketreservation.controller;

import com.neha.ticketreservation.dto.ShowRequest;
import com.neha.ticketreservation.dto.ShowResponse;
import com.neha.ticketreservation.service.ShowService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse createShow(@Valid @RequestBody ShowRequest request) {
        return showService.createShow(request);
    }

    @GetMapping("/{id}")
    public ShowResponse getShowById(@PathVariable Long id) {
        return showService.getShowById(id);
    }

    @GetMapping
    public Page<ShowResponse> getAllShows(Pageable pageable) {
        return showService.getAllShows(pageable);
    }

    @GetMapping("/search")
    public Page<ShowResponse> searchShows(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate showDate,
            Pageable pageable) {

        return showService.searchShows(
                movieId,
                showDate,
                pageable
        );
    }

    @PutMapping("/{id}")
    public ShowResponse updateShow(
            @PathVariable Long id,
            @Valid @RequestBody ShowRequest request) {

        return showService.updateShow(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
    }
}