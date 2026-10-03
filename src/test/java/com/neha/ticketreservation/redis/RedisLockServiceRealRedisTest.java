package com.neha.ticketreservation.redis;

import com.neha.ticketreservation.dto.BookingRequest;
import com.neha.ticketreservation.entity.Seat;
import com.neha.ticketreservation.entity.SeatStatus;
import com.neha.ticketreservation.entity.Show;
import com.neha.ticketreservation.entity.User;
import com.neha.ticketreservation.repository.BookingRepository;
import com.neha.ticketreservation.repository.BookingSeatRepository;
import com.neha.ticketreservation.repository.SeatRepository;
import com.neha.ticketreservation.repository.ShowRepository;
import com.neha.ticketreservation.repository.UserRepository;
import com.neha.ticketreservation.service.BookingServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Seat locking against a REAL Redis on localhost:6379 (no stand-in).
 *
 * If Redis is not running, these tests are skipped instead of failing,
 * so "mvnw test" still works on a machine without Docker.
 */
class RedisLockServiceRealRedisTest {

    private static final int USERS = 100;

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static boolean redisAvailable;

    private RedisLockService lockService;

    /** A fresh key per test so runs never interfere with each other or with real data. */
    private String seatKey;

    @BeforeAll
    static void connect() {

        try {

            connectionFactory = new LettuceConnectionFactory("localhost", 6379);
            connectionFactory.afterPropertiesSet();
            connectionFactory.start();

            redisTemplate = new StringRedisTemplate(connectionFactory);

            redisTemplate.getConnectionFactory().getConnection().ping();

            redisAvailable = true;

        } catch (Exception ex) {

            redisAvailable = false;
        }
    }

    @AfterAll
    static void disconnect() {

        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {

        Assumptions.assumeTrue(redisAvailable,
                "Redis is not running on localhost:6379, skipping real-Redis tests");

        lockService = new RedisLockService(redisTemplate);
        seatKey = "seat:test-" + UUID.randomUUID() + ":A1";
    }

    @AfterEach
    void cleanUp() {

        if (redisAvailable && seatKey != null) {
            redisTemplate.delete(seatKey);
        }
    }

    /** Runs the same task on 100 threads released at the same instant; returns how many returned true. */
    private int raceAndCountWinners(Callable<Boolean> task) throws Exception {

        AtomicInteger winners = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(USERS);
        CountDownLatch ready = new CountDownLatch(USERS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(USERS);

        for (int i = 0; i < USERS; i++) {

            pool.submit(() -> {

                ready.countDown();

                try {

                    start.await();

                    if (task.call()) {
                        winners.incrementAndGet();
                    }

                } catch (Exception ex) {

                    // a rejected booking counts as "did not win"

                } finally {

                    done.countDown();
                }
            });
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(done.await(60, TimeUnit.SECONDS));
        pool.shutdownNow();

        return winners.get();
    }

    @Test
    void oneHundredThreadsSameSeat_onlyOneGetsTheLock() throws Exception {

        int winners = raceAndCountWinners(() -> lockService.lockSeat(seatKey));

        assertEquals(1, winners);
    }

    @Test
    void lockExpiresOnItsOwnWithinFiveMinutes() {

        assertTrue(lockService.lockSeat(seatKey));

        Long secondsLeft = redisTemplate.getExpire(seatKey, TimeUnit.SECONDS);

        assertNotNull(secondsLeft);
        assertTrue(secondsLeft > 0 && secondsLeft <= 300,
                "lock should carry a time limit of at most 5 minutes, was " + secondsLeft);
    }

    @Test
    void seatCanBeLockedAgainAfterUnlock() {

        assertTrue(lockService.lockSeat(seatKey));
        assertFalse(lockService.lockSeat(seatKey));

        lockService.unlockSeat(seatKey);

        assertTrue(lockService.lockSeat(seatKey));
    }

    @Test
    void oneHundredUsersBookSameSeat_exactlyOneBookingSucceeds() throws Exception {

        // A show id no real show will have, so the Redis key cannot clash with real data
        long showId = 900_000_000L + (long) (Math.random() * 1_000_000);
        seatKey = "seat:" + showId + ":A1";

        Show show = new Show();
        show.setId(showId);
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
        when(seatRepository.findByShowIdAndSeatNumber(showId, "A1")).thenReturn(Optional.of(seat));

        BookingServiceImpl bookingService = new BookingServiceImpl(
                mock(BookingRepository.class),
                mock(BookingSeatRepository.class),
                seatRepository,
                showRepository,
                lockService,
                userRepository);

        int winners = raceAndCountWinners(() -> {

            BookingRequest request = new BookingRequest();
            request.setShowId(showId);
            request.setSeatNumbers(List.of("A1"));

            bookingService.createBooking(request, "user@example.com");

            return true;
        });

        assertEquals(1, winners, "exactly one user should get the seat");
        assertEquals(SeatStatus.LOCKED, seat.getStatus());
        verify(showRepository, times(1)).decrementAvailableSeats(showId, 1);

        // The winner's lock is still in Redis; the 99 losers must not have removed it
        assertEquals(Boolean.TRUE, redisTemplate.hasKey(seatKey));
    }
}
