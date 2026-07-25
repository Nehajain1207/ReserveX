package com.neha.ticketreservation.repository;

import com.neha.ticketreservation.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Movie> findByLanguageIgnoreCase(String language, Pageable pageable);

    Page<Movie> findByGenreIgnoreCase(String genre, Pageable pageable);

}