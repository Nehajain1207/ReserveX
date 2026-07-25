package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.AdminDashboardResponse;
import com.neha.ticketreservation.dto.BookingResponse;
import com.neha.ticketreservation.dto.RevenueResponse;
import com.neha.ticketreservation.entity.Booking;
import com.neha.ticketreservation.entity.BookingSeat;
import com.neha.ticketreservation.entity.BookingStatus;
import com.neha.ticketreservation.repository.BookingRepository;
import com.neha.ticketreservation.repository.BookingSeatRepository;
import com.neha.ticketreservation.repository.MovieRepository;
import com.neha.ticketreservation.repository.ShowRepository;
import com.neha.ticketreservation.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;

    public AdminServiceImpl(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            UserRepository userRepository,
            MovieRepository movieRepository,
            ShowRepository showRepository) {

        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
    }

    @Override
    public List<BookingResponse> getAllBookings() {

        List<Booking> bookings =
                bookingRepository.findAllByOrderByCreatedAtDesc();

        return bookings.stream()
                .map(booking -> {

                    int seatCount =
                            bookingSeatRepository.findByBooking(booking).size();

                    return new BookingResponse(
                            booking.getBookingReference(),
                            booking.getStatus().name(),
                            seatCount
                    );

                })
                .collect(Collectors.toList());
    }

    @Override
    public AdminDashboardResponse getDashboard() {

        long totalUsers = userRepository.count();

        long totalMovies = movieRepository.count();

        long totalShows = showRepository.count();

        long totalBookings = bookingRepository.count();

        long confirmedBookings =
                bookingRepository.findByStatus(BookingStatus.CONFIRMED).size();

        long pendingBookings =
                bookingRepository.findByStatus(BookingStatus.PENDING).size();

        long cancelledBookings =
                bookingRepository.findByStatus(BookingStatus.CANCELLED).size();

        return new AdminDashboardResponse(
                totalUsers,
                totalMovies,
                totalShows,
                totalBookings,
                confirmedBookings,
                pendingBookings,
                cancelledBookings
        );
    }

    @Override
    public RevenueResponse getRevenue() {

        BigDecimal totalRevenue = BigDecimal.ZERO;

        List<Booking> confirmedBookings =
                bookingRepository.findByStatus(BookingStatus.CONFIRMED);

        for (Booking booking : confirmedBookings) {

            List<BookingSeat> seats =
                    bookingSeatRepository.findByBooking(booking);

            BigDecimal ticketPrice =
                    booking.getShow().getTicketPrice();

            BigDecimal bookingRevenue =
                    ticketPrice.multiply(
                            BigDecimal.valueOf(seats.size())
                    );

            totalRevenue = totalRevenue.add(bookingRevenue);
        }

        return new RevenueResponse(totalRevenue);
    }
}