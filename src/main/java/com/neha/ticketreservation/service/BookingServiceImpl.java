package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.BookingRequest;
import com.neha.ticketreservation.dto.BookingResponse;
import com.neha.ticketreservation.entity.*;
import com.neha.ticketreservation.exception.ResourceNotFoundException;
import com.neha.ticketreservation.redis.RedisLockService;
import com.neha.ticketreservation.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final RedisLockService redisLockService;
    private final UserRepository userRepository;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            SeatRepository seatRepository,
            ShowRepository showRepository,
            RedisLockService redisLockService,
            UserRepository userRepository) {

        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.redisLockService = redisLockService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request, String email) {

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Show not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        Booking booking = new Booking();

        booking.setBookingReference(generateBookingReference());
        booking.setShow(show);
        booking.setUser(user);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        bookingRepository.save(booking);

        // Redis locks taken by THIS request. If anything fails part-way, the database
        // changes roll back with the transaction, but Redis is not part of that
        // transaction, so these locks must be released by hand.
        List<String> acquiredLocks = new ArrayList<>();

        try {

            for (String seatNumber : request.getSeatNumbers()) {

                String redisKey = "seat:" + show.getId() + ":" + seatNumber;

                boolean locked = redisLockService.lockSeat(redisKey);

                if (!locked) {
                    throw new RuntimeException(
                            "Seat " + seatNumber + " is currently being booked."
                    );
                }

                acquiredLocks.add(redisKey);

                Seat seat = seatRepository
                        .findByShowIdAndSeatNumber(show.getId(), seatNumber)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Seat not found : " + seatNumber));

                if (seat.getStatus() != SeatStatus.AVAILABLE) {
                    throw new RuntimeException(
                            "Seat " + seatNumber + " is not available."
                    );
                }

                seat.setStatus(SeatStatus.LOCKED);

                seatRepository.save(seat);

                BookingSeat bookingSeat = new BookingSeat();

                bookingSeat.setBooking(booking);
                bookingSeat.setSeat(seat);

                bookingSeatRepository.save(bookingSeat);
            }

        } catch (RuntimeException ex) {

            // Only release the locks this request took, never another user's lock
            acquiredLocks.forEach(redisLockService::unlockSeat);

            throw ex;
        }

        showRepository.decrementAvailableSeats(
                show.getId(),
                request.getSeatNumbers().size()
        );

        return new BookingResponse(
                booking.getBookingReference(),
                booking.getStatus().name(),
                request.getSeatNumbers().size()
        );
    }

    @Override
    public List<BookingResponse> getMyBookings(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        List<Booking> bookings = bookingRepository.findByUser(user);

        return bookings.stream()
                .map(booking -> {

                    int seatCount = bookingSeatRepository
                            .findByBooking(booking)
                            .size();

                    return new BookingResponse(
                            booking.getBookingReference(),
                            booking.getStatus().name(),
                            seatCount
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelBooking(
            String bookingReference,
            String email) {

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getEmail().equals(email)) {
            throw new RuntimeException(
                    "You are not authorized to cancel this booking."
            );
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException(
                    "Only confirmed bookings can be cancelled."
            );
        }

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findByBooking(booking);

        for (BookingSeat bookingSeat : bookingSeats) {

            Seat seat = bookingSeat.getSeat();

            seat.setStatus(SeatStatus.AVAILABLE);

            seatRepository.save(seat);

            String redisKey =
                    "seat:" + booking.getShow().getId()
                            + ":" + seat.getSeatNumber();

            redisLockService.unlockSeat(redisKey);
        }

        Show show = booking.getShow();

        showRepository.incrementAvailableSeats(
                show.getId(),
                bookingSeats.size()
        );

        booking.setStatus(BookingStatus.CANCELLED);

        bookingRepository.save(booking);
    }

    private String generateBookingReference() {

        return "RSVX-"
                + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}