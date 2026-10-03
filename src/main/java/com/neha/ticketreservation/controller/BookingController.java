package com.neha.ticketreservation.controller;

import com.neha.ticketreservation.dto.BookingRequest;
import com.neha.ticketreservation.dto.BookingResponse;
import com.neha.ticketreservation.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {

        return bookingService.createBooking(
                request,
                authentication.getName()
        );
    }

    @GetMapping("/my-bookings")
    public List<BookingResponse> getMyBookings(
             Authentication authentication) {
        return bookingService.getMyBookings(authentication.getName());
    }

    @DeleteMapping("/{bookingReference}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelBooking(
            @PathVariable String bookingReference,
            Authentication authentication) {

        bookingService.cancelBooking(
                bookingReference,
                authentication.getName()
        );
    }
}