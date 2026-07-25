package com.neha.ticketreservation.repository;

import com.neha.ticketreservation.entity.Booking;
import com.neha.ticketreservation.entity.BookingStatus;
import com.neha.ticketreservation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByUser(User user);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByStatusAndExpiresAtBefore(
            BookingStatus status,
            LocalDateTime time
    );

    // Admin APIs
    List<Booking> findAllByOrderByCreatedAtDesc();
}

