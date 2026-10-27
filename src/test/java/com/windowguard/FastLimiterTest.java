package com.windowguard;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FastLimiterTest {

    @Test
    void matchesReferenceOnRandomSequences() {
        for (int seed = 0; seed < 2000; seed++) {
            Random r = new Random(seed);
            ReferenceLimiter ref = new ReferenceLimiter();
            FastLimiter fast = new FastLimiter(20);
            String[] keys = {"A", "B", "C"};
            Map<String, Long> lastTime = new HashMap<>();

            for (int step = 0; step < 200; step++) {
                String key = keys[r.nextInt(keys.length)];
                long now = lastTime.getOrDefault(key, 0L) + r.nextInt(5);   // non-decreasing per key
                lastTime.put(key, now);
                int limit = r.nextInt(6);            // 0..5
                long window = 1 + r.nextInt(20);     // 1..20, changes every call
                int cost = 1 + r.nextInt(3);         // 1..3

                Decision a = ref.tryAcquire(key, limit, window, cost, now);
                Decision b = fast.tryAcquire(key, limit, window, cost, now);
                assertEquals(a, b, "seed=" + seed + " step=" + step + " key=" + key
                        + " now=" + now + " limit=" + limit + " window=" + window + " cost=" + cost);
            }
        }
    }

    @Test
    void longRunMatchesReference() {
        Random r = new Random(42);
        ReferenceLimiter ref = new ReferenceLimiter();
        FastLimiter fast = new FastLimiter(20);
        long now = 0;
        for (int i = 0; i < 20_000; i++) {
            now += r.nextInt(4);
            int limit = r.nextInt(8);
            long window = 1 + r.nextInt(20);
            int cost = 1 + r.nextInt(3);
            assertEquals(ref.tryAcquire("A", limit, window, cost, now),
                         fast.tryAcquire("A", limit, window, cost, now), "i=" + i);
        }
    }

    @Test
    void windowLargerThanMaxThrows() {
        FastLimiter l = new FastLimiter(20);
        assertThrows(IllegalArgumentException.class, () -> l.tryAcquire("A", 5, 21, 1, 0));
    }

    @Test
    void memoryStaysBounded() {
        FastLimiter l = new FastLimiter(20);
        for (long t = 0; t < 10_000; t++) {
            l.tryAcquire("A", 1_000_000, 20, 1, t);
        }
        assertTrue(l.liveEvents("A") <= 21, "live=" + l.liveEvents("A"));
    }
}