package com.neha.ticketreservation.controller;

import com.neha.ticketreservation.dto.MovieRequest;
import com.neha.ticketreservation.dto.MovieResponse;
import com.neha.ticketreservation.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse createMovie(@Valid @RequestBody MovieRequest request) {
        return movieService.createMovie(request);
    }

    @GetMapping("/{id}")
    public MovieResponse getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id);
    }

    @GetMapping
    public Page<MovieResponse> getAllMovies(Pageable pageable) {
        return movieService.getAllMovies(pageable);
    }

    @GetMapping("/search")
    public Page<MovieResponse> searchMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String genre,
            Pageable pageable) {

        return movieService.searchMovies(
                title,
                language,
                genre,
                pageable
        );
    }

    @PutMapping("/{id}")
    public MovieResponse updateMovie(
            @PathVariable Long id,
            @Valid @RequestBody MovieRequest request) {

        return movieService.updateMovie(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
    }
}