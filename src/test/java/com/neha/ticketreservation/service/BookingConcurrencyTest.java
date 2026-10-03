package com.neha.ticketreservation.service;

import com.neha.ticketreservation.dto.BookingRequest;
import com.neha.ticketreservation.entity.Seat;
import com.neha.ticketreservation.entity.SeatStatus;
import com.neha.ticketreservation.entity.Show;
import com.neha.ticketreservation.entity.User;
import com.neha.ticketreservation.redis.RedisLockService;
import com.neha.ticketreservation.repository.BookingRepository;
import com.neha.ticketreservation.repository.BookingSeatRepository;
import com.neha.ticketreservation.repository.SeatRepository;
import com.neha.ticketreservation.repository.ShowRepository;
import com.neha.ticketreservation.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Many users try to book the same seat at the same moment: exactly one may win.
 *
 * Redis is replaced by an in-memory stand-in with the same "set if absent"
 * behaviour, so this checks the booking logic around the lock, not Redis itself.
 */
class BookingConcurrencyTest {

    private static final int USERS = 100;

    /** Same contract as the real service: only the first caller for a key gets the lock. */
    static class InMemoryLockService extends RedisLockService {

        private final Set<String> locks = ConcurrentHashMap.newKeySet();

        InMemoryLockService() {
            super(null);
        }

        @Override
        public boolean lockSeat(String seatKey) {
            return locks.add(seatKey);
        }

        @Override
        public void unlockSeat(String seatKey) {
            locks.remove(seatKey);
        }

        int heldLocks() {
            return locks.size();
        }
    }

    @Test
    void oneHundredUsersSameSeat_exactlyOneBookingSucceeds() throws Exception {

        Show show = new Show();
        show.setId(1L);
        show.setAvailableSeats(100);

        Seat seat = new Seat();
        seat.setShow(show);
        seat.setSeatNumber("A1");
        seat.setStatus(SeatStatus.AVAILABLE);

        ShowRepository showRepository = mock(ShowRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        SeatRepository seatRepository = mock(SeatRepository.class);

        when(showRepository.findById(anyLong())).thenReturn(Optional.of(show));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));
        when(seatRepository.findByShowIdAndSeatNumber(1L, "A1")).thenReturn(Optional.of(seat));

        InMemoryLockService lockService = new InMemoryLockService();

        BookingServiceImpl bookingService = new BookingServiceImpl(
                mock(BookingRepository.class),
                mock(BookingSeatRepository.class),
                seatRepository,
                showRepository,
                lockService,
                userRepository);

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(USERS);
        CountDownLatch ready = new CountDownLatch(USERS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(USERS);

        for (int i = 0; i < USERS; i++) {

            String email = "user" + i + "@example.com";

            pool.submit(() -> {

                ready.countDown();

                try {

                    start.await();

                    BookingRequest request = new BookingRequest();
                    request.setShowId(1L);
                    request.setSeatNumbers(List.of("A1"));

                    bookingService.createBooking(request, email);

                    succeeded.incrementAndGet();

                } catch (RuntimeException ex) {

                    rejected.incrementAndGet();

                } catch (InterruptedException ex) {

                    Thread.currentThread().interrupt();

                } finally {

                    done.countDown();
                }
            });
        }

        // Release all 100 threads at the same instant
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(1, succeeded.get(), "exactly one user should get the seat");
        assertEquals(USERS - 1, rejected.get());
        assertEquals(SeatStatus.LOCKED, seat.getStatus());
        // The counter is reduced once, by the single winner
        verify(showRepository, times(1)).decrementAvailableSeats(1L, 1);

        // The winner still holds its lock; the 99 losers must not have removed it
        assertEquals(1, lockService.heldLocks());
    }
}
