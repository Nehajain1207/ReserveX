package com.neha.ticketreservation.repository;

import com.neha.ticketreservation.entity.Booking;
import com.neha.ticketreservation.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    List<BookingSeat> findByBooking(Booking booking);

}