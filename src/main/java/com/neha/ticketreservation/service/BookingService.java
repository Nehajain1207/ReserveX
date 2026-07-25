package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.BookingRequest;
import com.neha.ticketreservation.dto.BookingResponse;
import java.util.List;

public interface BookingService {

    BookingResponse createBooking(BookingRequest request, String email);
 List<BookingResponse> getMyBookings(String email);

 void cancelBooking(
         String bookingReference,
         String email
 );
}