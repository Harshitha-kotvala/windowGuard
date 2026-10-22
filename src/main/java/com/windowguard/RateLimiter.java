package com.windowguard;

public interface RateLimiter {

    Decision tryAcquire(
            String key,
            int limit,
            long windowMillis,
            int cost,
            long nowMillis
    );
}