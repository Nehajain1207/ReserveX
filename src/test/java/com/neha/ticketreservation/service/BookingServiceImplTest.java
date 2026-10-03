package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.BookingRequest;
import com.neha.ticketreservation.dto.BookingResponse;
import com.neha.ticketreservation.entity.Booking;
import com.neha.ticketreservation.entity.BookingSeat;
import com.neha.ticketreservation.entity.BookingStatus;
import com.neha.ticketreservation.entity.Seat;
import com.neha.ticketreservation.entity.SeatStatus;
import com.neha.ticketreservation.entity.Show;
import com.neha.ticketreservation.entity.User;
import com.neha.ticketreservation.exception.ResourceNotFoundException;
import com.neha.ticketreservation.redis.RedisLockService;
import com.neha.ticketreservation.repository.BookingRepository;
import com.neha.ticketreservation.repository.BookingSeatRepository;
import com.neha.ticketreservation.repository.SeatRepository;
import com.neha.ticketreservation.repository.ShowRepository;
import com.neha.ticketreservation.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Booking and seat-locking rules, with Redis and the database mocked.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final String EMAIL = "neha@example.com";

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ShowRepository showRepository;

    @Mock
    private RedisLockService redisLockService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Show show;
    private User user;

    @BeforeEach
    void setUp() {

        show = new Show();
        show.setId(1L);
        show.setTotalSeats(100);
        show.setAvailableSeats(100);

        user = new User();
        user.setEmail(EMAIL);
    }

    // ---------- helpers ----------

    private BookingRequest request(String... seatNumbers) {

        BookingRequest request = new BookingRequest();
        request.setShowId(1L);
        request.setSeatNumbers(List.of(seatNumbers));
        return request;
    }

    private Seat seat(String seatNumber, SeatStatus status) {

        Seat seat = new Seat();
        seat.setShow(show);
        seat.setSeatNumber(seatNumber);
        seat.setStatus(status);
        return seat;
    }

    private void showAndUserExist() {

        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private Booking booking(BookingStatus status, String ownerEmail) {

        User owner = new User();
        owner.setEmail(ownerEmail);

        Booking booking = new Booking();
        booking.setBookingReference("RSVX-TEST0001");
        booking.setShow(show);
        booking.setUser(owner);
        booking.setStatus(status);
        return booking;
    }

    // ---------- createBooking ----------

    @Test
    void createBooking_locksSeatsAndReturnsPendingBooking() {

        showAndUserExist();

        Seat a1 = seat("A1", SeatStatus.AVAILABLE);
        Seat a2 = seat("A2", SeatStatus.AVAILABLE);

        when(redisLockService.lockSeat(anyString())).thenReturn(true);
        when(seatRepository.findByShowIdAndSeatNumber(1L, "A1")).thenReturn(Optional.of(a1));
        when(seatRepository.findByShowIdAndSeatNumber(1L, "A2")).thenReturn(Optional.of(a2));

        BookingResponse response = bookingService.createBooking(request("A1", "A2"), EMAIL);

        assertEquals("PENDING", response.getStatus());
        assertEquals(2, response.getSeatsBooked());
        assertTrue(response.getBookingReference().startsWith("RSVX-"));

        assertEquals(SeatStatus.LOCKED, a1.getStatus());
        assertEquals(SeatStatus.LOCKED, a2.getStatus());
        verify(showRepository).decrementAvailableSeats(1L, 2);

        verify(redisLockService).lockSeat("seat:1:A1");
        verify(redisLockService).lockSeat("seat:1:A2");
        verify(bookingSeatRepository, times(2)).save(any(BookingSeat.class));
        verify(redisLockService, never()).unlockSeat(anyString());
    }

    @Test
    void createBooking_failsWhenSeatIsLockedByAnotherUser() {

        showAndUserExist();

        when(redisLockService.lockSeat("seat:1:A1")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request("A1"), EMAIL));

        assertTrue(ex.getMessage().contains("currently being booked"));

        // The lock belongs to someone else, so it must not be released
        verify(redisLockService, never()).unlockSeat(anyString());
        verify(seatRepository, never()).save(any(Seat.class));
        verify(showRepository, never()).decrementAvailableSeats(anyLong(), anyInt());
    }

    @Test
    void createBooking_releasesEarlierLocksWhenALaterSeatIsTaken() {

        showAndUserExist();

        Seat a1 = seat("A1", SeatStatus.AVAILABLE);
        Seat a2 = seat("A2", SeatStatus.AVAILABLE);

        when(redisLockService.lockSeat("seat:1:A1")).thenReturn(true);
        when(redisLockService.lockSeat("seat:1:A2")).thenReturn(true);
        when(redisLockService.lockSeat("seat:1:A3")).thenReturn(false);
        when(seatRepository.findByShowIdAndSeatNumber(1L, "A1")).thenReturn(Optional.of(a1));
        when(seatRepository.findByShowIdAndSeatNumber(1L, "A2")).thenReturn(Optional.of(a2));

        assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request("A1", "A2", "A3"), EMAIL));

        // A1 and A2 were locked by this request and must be freed again
        verify(redisLockService).unlockSeat("seat:1:A1");
        verify(redisLockService).unlockSeat("seat:1:A2");

        // A3 is held by another user and must be left alone
        verify(redisLockService, never()).unlockSeat("seat:1:A3");

        verify(showRepository, never()).decrementAvailableSeats(anyLong(), anyInt());
    }

    @Test
    void createBooking_releasesLockWhenSeatIsAlreadyBooked() {

        showAndUserExist();

        when(redisLockService.lockSeat("seat:1:A1")).thenReturn(true);
        when(seatRepository.findByShowIdAndSeatNumber(1L, "A1"))
                .thenReturn(Optional.of(seat("A1", SeatStatus.BOOKED)));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request("A1"), EMAIL));

        assertTrue(ex.getMessage().contains("not available"));

        verify(redisLockService).unlockSeat("seat:1:A1");
        verify(seatRepository, never()).save(any(Seat.class));
    }

    @Test
    void createBooking_releasesLockWhenSeatDoesNotExist() {

        showAndUserExist();

        when(redisLockService.lockSeat("seat:1:Z9")).thenReturn(true);
        when(seatRepository.findByShowIdAndSeatNumber(1L, "Z9")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createBooking(request("Z9"), EMAIL));

        verify(redisLockService).unlockSeat("seat:1:Z9");
    }

    @Test
    void createBooking_failsWhenShowDoesNotExist() {

        when(showRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createBooking(request("A1"), EMAIL));

        verify(redisLockService, never()).lockSeat(anyString());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBooking_failsWhenUserDoesNotExist() {

        when(showRepository.findById(1L)).thenReturn(Optional.of(show));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createBooking(request("A1"), EMAIL));

        verify(redisLockService, never()).lockSeat(anyString());
    }

    // ---------- cancelBooking ----------

    @Test
    void cancelBooking_freesSeatsAndLocksAndRestoresAvailability() {

        show.setAvailableSeats(98);

        Booking booking = booking(BookingStatus.CONFIRMED, EMAIL);

        Seat a1 = seat("A1", SeatStatus.BOOKED);
        Seat a2 = seat("A2", SeatStatus.BOOKED);

        BookingSeat first = new BookingSeat();
        first.setBooking(booking);
        first.setSeat(a1);

        BookingSeat second = new BookingSeat();
        second.setBooking(booking);
        second.setSeat(a2);

        when(bookingRepository.findByBookingReference("RSVX-TEST0001"))
                .thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findByBooking(booking)).thenReturn(List.of(first, second));

        bookingService.cancelBooking("RSVX-TEST0001", EMAIL);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(SeatStatus.AVAILABLE, a1.getStatus());
        assertEquals(SeatStatus.AVAILABLE, a2.getStatus());
        verify(showRepository).incrementAvailableSeats(1L, 2);

        verify(redisLockService).unlockSeat("seat:1:A1");
        verify(redisLockService).unlockSeat("seat:1:A2");
    }

    @Test
    void cancelBooking_rejectsAnotherUsersBooking() {

        Booking booking = booking(BookingStatus.CONFIRMED, "someone.else@example.com");

        when(bookingRepository.findByBookingReference("RSVX-TEST0001"))
                .thenReturn(Optional.of(booking));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.cancelBooking("RSVX-TEST0001", EMAIL));

        assertTrue(ex.getMessage().contains("not authorized"));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(redisLockService, never()).unlockSeat(anyString());
    }

    @Test
    void cancelBooking_rejectsBookingThatIsNotConfirmed() {

        Booking booking = booking(BookingStatus.PENDING, EMAIL);

        when(bookingRepository.findByBookingReference("RSVX-TEST0001"))
                .thenReturn(Optional.of(booking));

        assertThrows(RuntimeException.class,
                () -> bookingService.cancelBooking("RSVX-TEST0001", EMAIL));

        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void cancelBooking_failsWhenBookingDoesNotExist() {

        when(bookingRepository.findByBookingReference("RSVX-NOPE"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.cancelBooking("RSVX-NOPE", EMAIL));
    }
}
