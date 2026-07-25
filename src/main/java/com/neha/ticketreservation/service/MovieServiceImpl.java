package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.MovieRequest;
import com.neha.ticketreservation.dto.MovieResponse;
import com.neha.ticketreservation.entity.Movie;
import com.neha.ticketreservation.exception.ResourceNotFoundException;
import com.neha.ticketreservation.repository.MovieRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;

    public MovieServiceImpl(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
    @Transactional
    public MovieResponse createMovie(MovieRequest request) {

        Movie movie = new Movie();

        movie.setTitle(request.getTitle());
        movie.setLanguage(request.getLanguage());
        movie.setDuration(request.getDuration());
        movie.setGenre(request.getGenre());

        Movie savedMovie = movieRepository.save(movie);

        return mapToResponse(savedMovie);
    }

    @Override
    public MovieResponse getMovieById(Long id) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Movie not found"));

        return mapToResponse(movie);
    }

    @Override
    public Page<MovieResponse> getAllMovies(Pageable pageable) {

        return movieRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<MovieResponse> searchMovies(
            String title,
            String language,
            String genre,
            Pageable pageable) {

        if (title != null && !title.isBlank()) {
            return movieRepository
                    .findByTitleContainingIgnoreCase(title, pageable)
                    .map(this::mapToResponse);
        }

        if (language != null && !language.isBlank()) {
            return movieRepository
                    .findByLanguageIgnoreCase(language, pageable)
                    .map(this::mapToResponse);
        }

        if (genre != null && !genre.isBlank()) {
            return movieRepository
                    .findByGenreIgnoreCase(genre, pageable)
                    .map(this::mapToResponse);
        }

        return movieRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public MovieResponse updateMovie(Long id, MovieRequest request) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Movie not found"));

        movie.setTitle(request.getTitle());
        movie.setLanguage(request.getLanguage());
        movie.setDuration(request.getDuration());
        movie.setGenre(request.getGenre());

        Movie updatedMovie = movieRepository.save(movie);

        return mapToResponse(updatedMovie);
    }

    @Override
    @Transactional
    public void deleteMovie(Long id) {

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Movie not found"));

        movieRepository.delete(movie);
    }

    /**
     * Converts Movie Entity to MovieResponse DTO.
     */
    private MovieResponse mapToResponse(Movie movie) {

        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getLanguage(),
                movie.getDuration(),
                movie.getGenre()
        );
    }
}