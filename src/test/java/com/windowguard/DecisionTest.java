package com.windowguard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecisionTest {

    @Test
    void storesDecisionValues() {
        Decision decision = new Decision(true, 7, 0);

        assertTrue(decision.allowed());
        assertEquals(7, decision.remaining());
        assertEquals(0, decision.retryAfterMillis());
    }

    @Test
    void rejectsNegativeRemainingQuota() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Decision(true, -1, 0)
        );
    }

    @Test
    void rejectsNegativeRetryDelay() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Decision(false, 0, -1)
        );
    }
}