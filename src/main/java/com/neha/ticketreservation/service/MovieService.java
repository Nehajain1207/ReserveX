package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.MovieRequest;
import com.neha.ticketreservation.dto.MovieResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovieService {

    MovieResponse createMovie(MovieRequest request);

    MovieResponse getMovieById(Long id);

    Page<MovieResponse> getAllMovies(Pageable pageable);

    Page<MovieResponse> searchMovies(
            String title,
            String language,
            String genre,
            Pageable pageable);

    MovieResponse updateMovie(Long id, MovieRequest request);

    void deleteMovie(Long id);
}