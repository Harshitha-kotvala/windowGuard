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
            FastLimiter fast = new FastLimiter();
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
}