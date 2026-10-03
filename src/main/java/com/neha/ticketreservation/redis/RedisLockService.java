package com.neha.ticketreservation.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // -----------------------------
    // Seat Lock Methods
    // -----------------------------

    public boolean lockSeat(String seatKey) {

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(seatKey, "LOCKED", Duration.ofMinutes(5));

        return Boolean.TRUE.equals(success);
    }

    public void unlockSeat(String seatKey) {
        redisTemplate.delete(seatKey);
    }

    // -----------------------------
    // Idempotency Methods
    // -----------------------------

    public void saveValue(String key,
                          String value,
                          Duration duration) {

        redisTemplate.opsForValue().set(key, value, duration);
    }

    public String getValue(String key) {

        return redisTemplate.opsForValue().get(key);
    }

    public void deleteValue(String key) {

        redisTemplate.delete(key);
    }
}