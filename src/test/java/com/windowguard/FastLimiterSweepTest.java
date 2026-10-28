package com.windowguard;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class FastLimiterSweepTest {

    @Test
    void sweepRemovesIdleKeysOnly() {
        FastLimiter l = new FastLimiter(20);
        for (int i = 0; i < 1000; i++) l.tryAcquire("idle-" + i, 5, 10, 1, 0);
        l.tryAcquire("active", 5, 10, 1, 95);
        assertEquals(1001, l.keyCount());

        int removed = l.sweep(100);          // cutoff 80: events at 0 are dead, event at 95 is live

        assertEquals(1000, removed);
        assertEquals(1, l.keyCount());
        assertEquals(1, l.liveEvents("active"));
    }

    @Test
    void keyWorksNormallyAfterSweep() {
        FastLimiter l = new FastLimiter(20);
        l.tryAcquire("A", 1, 10, 1, 0);
        l.sweep(100);
        assertTrue(l.tryAcquire("A", 1, 10, 1, 100).allowed());
    }

    @Test
    void sweeperNeverLosesAcceptedRequests() throws Exception {
        int threads = 50, limit = 10;
        for (int round = 0; round < 300; round++) {
            FastLimiter limiter = new FastLimiter(20);
            ExecutorService pool = Executors.newFixedThreadPool(threads + 1);
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch go = new CountDownLatch(1);
            AtomicInteger allowed = new AtomicInteger();
            AtomicBoolean stop = new AtomicBoolean(false);

            // sweeps continuously while requests arrive; empty histories look idle, so races happen
            Future<?> sweeper = pool.submit(() -> {
                while (!stop.get()) limiter.sweep(100);
            });

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    if (limiter.tryAcquire("A", limit, 10, 1, 100).allowed()) allowed.incrementAndGet();
                    return null;
                }));
            }
            ready.await();
            go.countDown();
            for (Future<?> f : futures) f.get();
            stop.set(true);
            sweeper.get();
            pool.shutdown();

            assertEquals(limit, allowed.get(), "round " + round);
        }
    }
}