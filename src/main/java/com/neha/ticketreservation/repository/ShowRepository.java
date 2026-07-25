package com.neha.ticketreservation.repository;

import com.neha.ticketreservation.entity.Show;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ShowRepository extends JpaRepository<Show, Long> {

    Page<Show> findByMovieId(Long movieId, Pageable pageable);

    Page<Show> findByShowDate(LocalDate showDate, Pageable pageable);

}