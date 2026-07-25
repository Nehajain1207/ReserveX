package com.neha.ticketreservation.repository;

import com.neha.ticketreservation.entity.Seat;
import com.neha.ticketreservation.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import com.neha.ticketreservation.entity.SeatStatus;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShow(Show show);
    List<Seat> findByShowIdAndStatus(Long showId, SeatStatus status);

    Optional<Seat> findByShowIdAndSeatNumber(Long showId, String seatNumber);
}