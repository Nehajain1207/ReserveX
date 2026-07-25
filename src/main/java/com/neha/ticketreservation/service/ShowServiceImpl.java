package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.ShowRequest;
import com.neha.ticketreservation.dto.ShowResponse;
import com.neha.ticketreservation.entity.Movie;
import com.neha.ticketreservation.entity.Seat;
import com.neha.ticketreservation.entity.SeatStatus;
import com.neha.ticketreservation.entity.Show;
import com.neha.ticketreservation.exception.ResourceNotFoundException;
import com.neha.ticketreservation.repository.MovieRepository;
import com.neha.ticketreservation.repository.SeatRepository;
import com.neha.ticketreservation.repository.ShowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ShowServiceImpl implements ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final SeatRepository seatRepository;

    public ShowServiceImpl(
            ShowRepository showRepository,
            MovieRepository movieRepository,
            SeatRepository seatRepository) {

        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    @Transactional
    public ShowResponse createShow(ShowRequest request) {

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Movie not found"));

        Show show = new Show();

        show.setMovie(movie);
        show.setShowDate(request.getShowDate());
        show.setShowTime(request.getShowTime());
        show.setScreenNumber(request.getScreenNumber());
        show.setTicketPrice(request.getTicketPrice());

        show.setTotalSeats(120);
        show.setAvailableSeats(120);

        Show savedShow = showRepository.save(show);

        for (char row = 'A'; row <= 'J'; row++) {

            for (int col = 1; col <= 12; col++) {

                Seat seat = new Seat();

                seat.setShow(savedShow);
                seat.setSeatNumber(row + String.valueOf(col));
                seat.setStatus(SeatStatus.AVAILABLE);

                seatRepository.save(seat);
            }
        }

        return mapToResponse(savedShow);
    }

    @Override
    public ShowResponse getShowById(Long id) {

        Show show = showRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Show not found"));

        return mapToResponse(show);
    }

    @Override
    public Page<ShowResponse> getAllShows(Pageable pageable) {

        return showRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<ShowResponse> searchShows(
            Long movieId,
            LocalDate showDate,
            Pageable pageable) {

        if (movieId != null) {
            return showRepository.findByMovieId(movieId, pageable)
                    .map(this::mapToResponse);
        }

        if (showDate != null) {
            return showRepository.findByShowDate(showDate, pageable)
                    .map(this::mapToResponse);
        }

        return showRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ShowResponse updateShow(Long id, ShowRequest request) {

        Show show = showRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Show not found"));

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Movie not found"));

        show.setMovie(movie);
        show.setShowDate(request.getShowDate());
        show.setShowTime(request.getShowTime());
        show.setScreenNumber(request.getScreenNumber());
        show.setTicketPrice(request.getTicketPrice());

        Show updatedShow = showRepository.save(show);

        return mapToResponse(updatedShow);
    }

    @Override
    @Transactional
    public void deleteShow(Long id) {

        Show show = showRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Show not found"));

        showRepository.delete(show);
    }

    private ShowResponse mapToResponse(Show show) {

        return new ShowResponse(
                show.getId(),
                show.getMovie().getTitle(),
                show.getShowDate(),
                show.getShowTime(),
                show.getScreenNumber(),
                show.getTotalSeats(),
                show.getAvailableSeats(),
                show.getTicketPrice()
        );
    }
}