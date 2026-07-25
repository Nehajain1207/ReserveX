package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.AuditoriumRequest;
import com.neha.ticketreservation.entity.Seat;
import com.neha.ticketreservation.entity.SeatStatus;
import com.neha.ticketreservation.entity.Show;
import com.neha.ticketreservation.repository.SeatRepository;
import com.neha.ticketreservation.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;

    public SeatServiceImpl(SeatRepository seatRepository,
                           ShowRepository showRepository) {
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
    }

    @Override
    @Transactional
    public void initializeAuditorium(Long showId, AuditoriumRequest request) {

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new RuntimeException("Show not found"));

        int totalSeats = 0;

        for (int row = 0; row < request.getRows(); row++) {

            char rowLetter = (char) ('A' + row);

            for (int seat = 1; seat <= request.getSeatsPerRow(); seat++) {

                Seat newSeat = new Seat();

                newSeat.setShow(show);
                newSeat.setSeatNumber(rowLetter + String.valueOf(seat));
                newSeat.setStatus(SeatStatus.AVAILABLE);

                seatRepository.save(newSeat);

                totalSeats++;
            }
        }

        show.setTotalSeats(totalSeats);
        show.setAvailableSeats(totalSeats);

        showRepository.save(show);
    }

    @Override
    public List<String> getAvailableSeats(Long showId) {

        List<Seat> seats =
                seatRepository.findByShowIdAndStatus(showId, SeatStatus.AVAILABLE);

        List<String> availableSeats = new ArrayList<>();

        for (Seat seat : seats) {
            availableSeats.add(seat.getSeatNumber());
        }

        return availableSeats;
    }
}