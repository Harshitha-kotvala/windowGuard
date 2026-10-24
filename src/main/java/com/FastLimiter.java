package com.windowguard;

import java.util.*;

public class FastLimiter implements RateLimiter {

    private static final class History {
        long[] times = new long[8];
        long[] cum = new long[8];
        int size = 0;

        long total() { return size == 0 ? 0 : cum[size - 1]; }
        long sumBefore(int i) { return i == 0 ? 0 : cum[i - 1]; }

        /** first index whose time is strictly greater than cutoff */
        int firstAfter(long cutoff) {
            int lo = 0, hi = size;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (times[mid] > cutoff) hi = mid; else lo = mid + 1;
            }
            return lo;
        }

        /** first index j >= from with cum[j] - base >= target */
        int firstReaching(int from, long base, long target) {
            int lo = from, hi = size;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (cum[mid] - base >= target) hi = mid; else lo = mid + 1;
            }
            return lo;
        }

        void add(long t, int cost) {
            if (size == times.length) {
                times = Arrays.copyOf(times, size * 2);
                cum = Arrays.copyOf(cum, size * 2);
            }
            times[size] = t;
            cum[size] = total() + cost;
            size++;
        }
    }

    private final Map<String, History> histories = new HashMap<>();

    @Override
    public Decision tryAcquire(String key, int limit, long windowMillis, int cost, long now) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key");
        if (limit < 0) throw new IllegalArgumentException("limit");
        if (windowMillis <= 0) throw new IllegalArgumentException("window");
        if (cost <= 0) throw new IllegalArgumentException("cost");

        History h = histories.computeIfAbsent(key, k -> new History());
        if (h.size > 0 && now < h.times[h.size - 1])
            throw new IllegalArgumentException("timestamp went backwards");

        int first = h.firstAfter(now - windowMillis);
        long base = h.sumBefore(first);
        long used = h.total() - base;

        if (used + cost <= limit) {
            h.add(now, cost);
            return new Decision(true, (int) (limit - used - cost), 0);
        }

        int remaining = (int) Math.max(0, limit - used);
        if (cost > limit) return new Decision(false, remaining, Decision.NEVER);

        long need = used + cost - limit;
        int j = h.firstReaching(first, base, need);
        return new Decision(false, remaining, h.times[j] + windowMillis - now);
    }
}