package com.neha.ticketreservation.scheduler;

import com.neha.ticketreservation.entity.*;
import com.neha.ticketreservation.redis.RedisLockService;
import com.neha.ticketreservation.repository.BookingRepository;
import com.neha.ticketreservation.repository.BookingSeatRepository;
import com.neha.ticketreservation.repository.SeatRepository;
import com.neha.ticketreservation.repository.ShowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookingExpiryScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(BookingExpiryScheduler.class);

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final RedisLockService redisLockService;

    public BookingExpiryScheduler(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            SeatRepository seatRepository,
            ShowRepository showRepository,
            RedisLockService redisLockService) {

        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.redisLockService = redisLockService;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireBookings() {

        List<Booking> pendingBookings =
                bookingRepository.findByStatusAndExpiresAtBefore(
                        BookingStatus.PENDING,
                        LocalDateTime.now()
                );

        for (Booking booking : pendingBookings) {

            booking.setStatus(BookingStatus.EXPIRED);

            List<BookingSeat> bookingSeats =
                    bookingSeatRepository.findByBooking(booking);

            int unlockedSeats = 0;

            Show show = booking.getShow();

            for (BookingSeat bookingSeat : bookingSeats) {

                Seat seat = bookingSeat.getSeat();

                // Make seat available again
                seat.setStatus(SeatStatus.AVAILABLE);
                seatRepository.save(seat);

                // Remove Redis lock immediately
                String redisKey =
                        "seat:" + show.getId() + ":" + seat.getSeatNumber();

                redisLockService.unlockSeat(redisKey);

                unlockedSeats++;
            }

            show.setAvailableSeats(
                    show.getAvailableSeats() + unlockedSeats
            );

            showRepository.save(show);

            bookingRepository.save(booking);

            log.info(
                    "Booking {} expired. {} seats released.",
                    booking.getBookingReference(),
                    unlockedSeats
            );
        }
    }
}