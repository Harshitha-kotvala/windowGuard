package com.windowguard;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class FastLimiterConcurrencyTest {

    @Test
    void neverAdmitsMoreThanLimitOnOneKey() throws Exception {
        int threads = 100, limit = 10;
        for (int round = 0; round < 200; round++) {
            FastLimiter limiter = new FastLimiter(20);
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch go = new CountDownLatch(1);
            AtomicInteger allowed = new AtomicInteger();
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    // same "now" for everyone, so no thread looks like it went backwards
                    if (limiter.tryAcquire("A", limit, 10, 1, 100).allowed()) allowed.incrementAndGet();
                    return null;
                }));
            }
            ready.await();
            go.countDown();
            for (Future<?> f : futures) f.get();
            pool.shutdown();

            assertEquals(limit, allowed.get(), "round " + round);
        }
    }

    @Test
    void differentKeysDoNotInterfere() throws Exception {
        int keys = 20, perKey = 50, limit = 5;
        FastLimiter limiter = new FastLimiter(20);
        ExecutorService pool = Executors.newFixedThreadPool(32);
        Map<String, AtomicInteger> allowed = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int k = 0; k < keys; k++) {
            String key = "key-" + k;
            allowed.put(key, new AtomicInteger());
            for (int i = 0; i < perKey; i++) {
                futures.add(pool.submit(() -> {
                    if (limiter.tryAcquire(key, limit, 10, 1, 100).allowed()) allowed.get(key).incrementAndGet();
                    return null;
                }));
            }
        }
        for (Future<?> f : futures) f.get();
        pool.shutdown();

        for (int k = 0; k < keys; k++) {
            assertEquals(limit, allowed.get("key-" + k).get(), "key-" + k);
        }
    }
}