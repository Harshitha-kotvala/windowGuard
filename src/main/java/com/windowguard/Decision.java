package com.windowguard;

/**
 * The outcome of a rate-limit acquisition attempt.
 *
 * @param allowed whether the request was accepted
 * @param remaining available quota after the decision
 * @param retryAfterMillis milliseconds until a retry may succeed
 */
public record Decision(
        boolean allowed,
        int remaining,
        long retryAfterMillis
) {
    public Decision {
        if (remaining < 0) {
            throw new IllegalArgumentException(
                    "remaining cannot be negative"
            );
        }

        if (retryAfterMillis < 0) {
            throw new IllegalArgumentException(
                    "retryAfterMillis cannot be negative"
            );
        }
    }
}