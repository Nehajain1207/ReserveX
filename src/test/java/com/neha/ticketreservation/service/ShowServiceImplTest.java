package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.ShowRequest;
import com.neha.ticketreservation.dto.ShowResponse;
import com.neha.ticketreservation.entity.Movie;
import com.neha.ticketreservation.entity.Show;
import com.neha.ticketreservation.exception.ResourceNotFoundException;
import com.neha.ticketreservation.repository.MovieRepository;
import com.neha.ticketreservation.repository.SeatRepository;
import com.neha.ticketreservation.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowServiceImplTest {

    @Mock
    private ShowRepository showRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private ShowServiceImpl showService;

    private Movie movie;
    private Show show;
    private ShowRequest request;

    @BeforeEach
    void setUp() {

        movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Interstellar");

        show = new Show();
        show.setId(1L);
        show.setMovie(movie);
        show.setShowDate(LocalDate.now());
        show.setShowTime(LocalTime.of(18,30));
        show.setScreenNumber(1);
        show.setTotalSeats(120);
        show.setAvailableSeats(120);
        show.setTicketPrice(BigDecimal.valueOf(250));

        request = new ShowRequest();
        request.setMovieId(1L);
        request.setShowDate(LocalDate.now());
        request.setShowTime(LocalTime.of(18,30));
        request.setScreenNumber(1);
        request.setTicketPrice(BigDecimal.valueOf(250));
    }

    @Test
    void createShow_ShouldReturnShowResponse() {

        when(movieRepository.findById(1L))
                .thenReturn(Optional.of(movie));

        when(showRepository.save(any(Show.class)))
                .thenReturn(show);

        ShowResponse response =
                showService.createShow(request);

        assertNotNull(response);
        assertEquals("Interstellar",
                response.getMovieTitle());

        verify(movieRepository).findById(1L);
        verify(showRepository).save(any(Show.class));

        verify(seatRepository, times(120))
                .save(any());
    }

    @Test
    void getShowById_ShouldReturnShow() {

        when(showRepository.findById(1L))
                .thenReturn(Optional.of(show));

        ShowResponse response =
                showService.getShowById(1L);

        assertEquals("Interstellar",
                response.getMovieTitle());

        verify(showRepository).findById(1L);
    }

    @Test
    void getShowById_ShouldThrowException() {

        when(showRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> showService.getShowById(1L));

        verify(showRepository).findById(1L);
    }

    @Test
    void updateShow_ShouldUpdateSuccessfully() {

        when(showRepository.findById(1L))
                .thenReturn(Optional.of(show));

        when(movieRepository.findById(1L))
                .thenReturn(Optional.of(movie));

        when(showRepository.save(any(Show.class)))
                .thenReturn(show);

        ShowResponse response =
                showService.updateShow(1L, request);

        assertEquals("Interstellar",
                response.getMovieTitle());

        verify(showRepository).save(any(Show.class));
    }

    @Test
void getAllShows_ShouldReturnPage() {

    Pageable pageable = PageRequest.of(0, 5);

    Page<Show> page = new PageImpl<>(List.of(show));

    when(showRepository.findAll(pageable))
            .thenReturn(page);

    Page<ShowResponse> result =
            showService.getAllShows(pageable);

    assertEquals(1, result.getTotalElements());
    assertEquals("Interstellar",
            result.getContent().get(0).getMovieTitle());

    verify(showRepository).findAll(pageable);
}

@Test
void searchShows_ByMovieId_ShouldReturnShows() {

    Pageable pageable = PageRequest.of(0, 5);

    Page<Show> page = new PageImpl<>(List.of(show));

    when(showRepository.findByMovieId(1L, pageable))
            .thenReturn(page);

    Page<ShowResponse> result =
            showService.searchShows(
                    1L,
                    null,
                    pageable);

    assertEquals(1, result.getTotalElements());

    verify(showRepository)
            .findByMovieId(1L, pageable);
}

@Test
void searchShows_ByDate_ShouldReturnShows() {

    Pageable pageable = PageRequest.of(0, 5);

    Page<Show> page = new PageImpl<>(List.of(show));

    when(showRepository.findByShowDate(
            LocalDate.now(),
            pageable))
            .thenReturn(page);

    Page<ShowResponse> result =
            showService.searchShows(
                    null,
                    LocalDate.now(),
                    pageable);

    assertEquals(1, result.getTotalElements());

    verify(showRepository)
            .findByShowDate(LocalDate.now(), pageable);
}

@Test
void searchShows_WithoutFilters_ShouldReturnAllShows() {

    Pageable pageable = PageRequest.of(0, 5);

    Page<Show> page = new PageImpl<>(List.of(show));

    when(showRepository.findAll(pageable))
            .thenReturn(page);

    Page<ShowResponse> result =
            showService.searchShows(
                    null,
                    null,
                    pageable);

    assertEquals(1, result.getTotalElements());

    verify(showRepository)
            .findAll(pageable);
}

@Test
void deleteShow_ShouldDeleteSuccessfully() {

    when(showRepository.findById(1L))
            .thenReturn(Optional.of(show));

    showService.deleteShow(1L);

    verify(showRepository).delete(show);
}

@Test
void deleteShow_ShouldThrowException() {

    when(showRepository.findById(1L))
            .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
            () -> showService.deleteShow(1L));

    verify(showRepository).findById(1L);
}
}