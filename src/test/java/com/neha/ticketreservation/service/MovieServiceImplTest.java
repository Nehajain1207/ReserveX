package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.MovieRequest;
import com.neha.ticketreservation.dto.MovieResponse;
import com.neha.ticketreservation.entity.Movie;
import com.neha.ticketreservation.exception.ResourceNotFoundException;
import com.neha.ticketreservation.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieServiceImpl movieService;

    private Movie movie;
    private MovieRequest request;

    @BeforeEach
    void setUp() {

        movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Interstellar");
        movie.setLanguage("English");
        movie.setDuration(169);
        movie.setGenre("Sci-Fi");

        request = new MovieRequest();
        request.setTitle("Interstellar");
        request.setLanguage("English");
        request.setDuration(169);
        request.setGenre("Sci-Fi");
    }
    @Test
    void createMovie_ShouldReturnMovieResponse() {

        // Arrange
        when(movieRepository.save(any(Movie.class)))
                .thenReturn(movie);

        // Act
        MovieResponse response = movieService.createMovie(request);

        // Assert
        assertNotNull(response);
        assertEquals("Interstellar", response.getTitle());
        assertEquals("English", response.getLanguage());

        verify(movieRepository, times(1))
                .save(any(Movie.class));
    }
    @Test
    void getMovieById_ShouldReturnMovie() {

        // Arrange
        when(movieRepository.findById(1L))
                .thenReturn(Optional.of(movie));

        // Act
        MovieResponse response = movieService.getMovieById(1L);

        // Assert
        assertNotNull(response);
        assertEquals("Interstellar", response.getTitle());
        assertEquals("English", response.getLanguage());

        // Verify
        verify(movieRepository).findById(1L);
    }

    @Test
    void getMovieById_ShouldThrowException_WhenMovieNotFound() {

        // Arrange
        when(movieRepository.findById(1L))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class,
                () -> movieService.getMovieById(1L));

        // Verify
        verify(movieRepository).findById(1L);
    }
    @Test
    void updateMovie_ShouldUpdateMovieSuccessfully() {

        // Arrange
        when(movieRepository.findById(1L))
                .thenReturn(Optional.of(movie));

        when(movieRepository.save(any(Movie.class)))
                .thenReturn(movie);

        // Act
        MovieResponse response = movieService.updateMovie(1L, request);

        // Assert
        assertNotNull(response);
        assertEquals("Interstellar", response.getTitle());

        // Verify
        verify(movieRepository).findById(1L);
        verify(movieRepository).save(any(Movie.class));
    }
    @Test
    void deleteMovie_ShouldDeleteMovieSuccessfully() {

        // Arrange
        when(movieRepository.findById(1L))
                .thenReturn(Optional.of(movie));

        // Act
        movieService.deleteMovie(1L);

        // Verify
        verify(movieRepository).findById(1L);
        verify(movieRepository).delete(movie);
    }

    @Test
    void searchMovies_ByTitle_ShouldReturnMovies() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 5);

        Page<Movie> page =
                new PageImpl<>(List.of(movie));

        when(movieRepository.findByTitleContainingIgnoreCase(
                "Inter", pageable))
                .thenReturn(page);

        // Act
        Page<MovieResponse> result =
                movieService.searchMovies(
                        "Inter",
                        null,
                        null,
                        pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Interstellar",
                result.getContent().get(0).getTitle());

        // Verify
        verify(movieRepository)
                .findByTitleContainingIgnoreCase("Inter", pageable);
    }

    @Test
    void searchMovies_ByLanguage_ShouldReturnMovies() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 5);

        Page<Movie> page = new PageImpl<>(List.of(movie));

        when(movieRepository.findByLanguageIgnoreCase(
                "English", pageable))
                .thenReturn(page);

        // Act
        Page<MovieResponse> result =
                movieService.searchMovies(
                        null,
                        "English",
                        null,
                        pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("English",
                result.getContent().get(0).getLanguage());

        // Verify
        verify(movieRepository)
                .findByLanguageIgnoreCase("English", pageable);
    }
    @Test
    void searchMovies_ByGenre_ShouldReturnMovies() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 5);

        Page<Movie> page = new PageImpl<>(List.of(movie));

        when(movieRepository.findByGenreIgnoreCase(
                "Sci-Fi", pageable))
                .thenReturn(page);

        // Act
        Page<MovieResponse> result =
                movieService.searchMovies(
                        null,
                        null,
                        "Sci-Fi",
                        pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Sci-Fi",
                result.getContent().get(0).getGenre());

        // Verify
        verify(movieRepository)
                .findByGenreIgnoreCase("Sci-Fi", pageable);
    }
    }