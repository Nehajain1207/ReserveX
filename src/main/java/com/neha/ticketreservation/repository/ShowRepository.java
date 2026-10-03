package com.neha.ticketreservation.repository;

import com.neha.ticketreservation.entity.Show;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ShowRepository extends JpaRepository<Show, Long> {

    Page<Show> findByMovieId(Long movieId, Pageable pageable);

    Page<Show> findByShowDate(LocalDate showDate, Pageable pageable);

    // The seat counter is changed with a single UPDATE inside the database
    // ("available = available - n"). Reading the number in Java, changing it and
    // saving it back loses updates when many bookings run at the same time.

    @Modifying
    @Query("update Show s set s.availableSeats = s.availableSeats - :count where s.id = :showId")
    int decrementAvailableSeats(@Param("showId") Long showId, @Param("count") int count);

    @Modifying
    @Query("update Show s set s.availableSeats = s.availableSeats + :count where s.id = :showId")
    int incrementAvailableSeats(@Param("showId") Long showId, @Param("count") int count);

}
