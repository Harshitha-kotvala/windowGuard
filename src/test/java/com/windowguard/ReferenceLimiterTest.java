package com.windowguard;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReferenceLimiterTest {
    @Test
    void slidingWindowExample() {
        ReferenceLimiter l = new ReferenceLimiter();
        long[] times =       {1,    2,    5,    8,     11,   12,   13};
        boolean[] expected = {true, true, true, false, true, true, false};
        for (int i = 0; i < times.length; i++) {
            assertEquals(expected[i], l.tryAcquire("A", 3, 10, 1, times[i]).allowed(), "t=" + times[i]);
        }
    }

    @Test
    void clientsAreIndependent() {
        ReferenceLimiter l = new ReferenceLimiter();
        assertTrue(l.tryAcquire("A", 1, 10, 1, 1).allowed());
        assertFalse(l.tryAcquire("A", 1, 10, 1, 2).allowed());
        assertTrue(l.tryAcquire("B", 1, 10, 1, 2).allowed());
    }

    @Test
    void boundaryIsExclusive() {
        ReferenceLimiter l = new ReferenceLimiter();
        assertTrue(l.tryAcquire("A", 1, 10, 1, 0).allowed());
        assertFalse(l.tryAcquire("A", 1, 10, 1, 9).allowed());   // 0 > -1, still inside
        assertTrue(l.tryAcquire("A", 1, 10, 1, 10).allowed());   // 0 > 0 false, expired
    }
}