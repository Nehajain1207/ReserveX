package com.neha.ticketreservation.service;

import com.neha.ticketreservation.redis.RedisLockService;
import com.neha.ticketreservation.security.JwtUtil;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
public class LogoutServiceImpl implements LogoutService {

    private final RedisLockService redisLockService;
    private final JwtUtil jwtUtil;

    public LogoutServiceImpl(
            RedisLockService redisLockService,
            JwtUtil jwtUtil) {

        this.redisLockService = redisLockService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void logout(String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid Authorization Header");
        }

        String token = authHeader.substring(7);

        Date expiry = jwtUtil.extractExpiration(token);

        long remainingTime =
                expiry.getTime() - System.currentTimeMillis();

        if (remainingTime > 0) {

            redisLockService.saveValue(
                    "blacklist:" + token,
                    "LOGGED_OUT",
                    Duration.ofMillis(remainingTime)
            );
        }
    }
}