package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.PaymentResponse;
import com.neha.ticketreservation.entity.Booking;
import com.neha.ticketreservation.entity.BookingSeat;
import com.neha.ticketreservation.entity.BookingStatus;
import com.neha.ticketreservation.entity.Seat;
import com.neha.ticketreservation.entity.SeatStatus;
import com.neha.ticketreservation.redis.RedisLockService;
import com.neha.ticketreservation.repository.BookingRepository;
import com.neha.ticketreservation.repository.BookingSeatRepository;
import com.neha.ticketreservation.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final RedisLockService redisLockService;

    public PaymentServiceImpl(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            SeatRepository seatRepository,
            RedisLockService redisLockService) {

        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.redisLockService = redisLockService;
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(
            String bookingReference,
            String userEmail,
            String idempotencyKey) {

        String paymentKey = "payment:" + idempotencyKey;

        // Check if this payment request has already been processed
        String existingResponse = redisLockService.getValue(paymentKey);

        if (existingResponse != null) {

            return new PaymentResponse(
                    bookingReference,
                    "SUCCESS",
                    existingResponse
            );
        }

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException(
                    "You are not authorized to pay for this booking."
            );
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException(
                    "Payment cannot be completed because booking status is "
                            + booking.getStatus()
            );
        }

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findByBooking(booking);

        for (BookingSeat bookingSeat : bookingSeats) {

            Seat seat = bookingSeat.getSeat();

            seat.setStatus(SeatStatus.BOOKED);

            seatRepository.save(seat);

            // Remove Redis lock immediately after payment
            String redisKey =
                    "seat:" + booking.getShow().getId() + ":" + seat.getSeatNumber();

            redisLockService.unlockSeat(redisKey);
        }

        booking.setStatus(BookingStatus.CONFIRMED);

        bookingRepository.save(booking);

        PaymentResponse response = new PaymentResponse(
                booking.getBookingReference(),
                "SUCCESS",
                "Payment successful. Booking confirmed."
        );

        // Store the payment response in Redis for 30 minutes
        redisLockService.saveValue(
                paymentKey,
                response.getMessage(),
                Duration.ofMinutes(30)
        );

        return response;
    }
}