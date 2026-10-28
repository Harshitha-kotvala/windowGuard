package com.windowguard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FastLimiter implements RateLimiter {

    private static final class History {
        long[] times = new long[8];
        long[] cum = new long[8];   // cum[i] = total cost of array slots 0..i
        int head = 0;               // live events are slots [head, size)
        int size = 0;
        long lastTime = Long.MIN_VALUE;   // last accepted timestamp, survives eviction
        boolean retired = false;          // set by the sweeper, read under the lock

        long total() { return size == 0 ? 0 : cum[size - 1]; }
        long sumBefore(int i) { return i == 0 ? 0 : cum[i - 1]; }
        int live() { return size - head; }

        /** first index in [head, size) whose time is strictly greater than cutoff */
        int firstAfter(long cutoff) {
            int lo = head, hi = size;
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

        /** drop events no legal window can still count */
        void evict(long cutoff) {
            head = firstAfter(cutoff);
        }

        void add(long t, int cost) {
            if (size == times.length) {
                if (head > times.length / 2) compact();   // lots of dead space: reuse it
                else grow();                              // mostly live: double
            }
            times[size] = t;
            cum[size] = total() + cost;
            size++;
            lastTime = t;
        }

        private void compact() {
            long offset = sumBefore(head);     // cost of all dead events
            int live = size - head;
            System.arraycopy(times, head, times, 0, live);
            for (int i = 0; i < live; i++) {
                cum[i] = cum[head + i] - offset;   // rebase so live part starts at 0
            }
            head = 0;
            size = live;
        }

        private void grow() {
            times = Arrays.copyOf(times, times.length * 2);
            cum = Arrays.copyOf(cum, cum.length * 2);
        }
    }

    private final long maxWindow;
    private final Map<String, History> histories = new ConcurrentHashMap<>();

    public FastLimiter(long maxWindow) {
        if (maxWindow <= 0) throw new IllegalArgumentException("maxWindow");
        this.maxWindow = maxWindow;
    }

    @Override
    public Decision tryAcquire(String key, int limit, long windowMillis, int cost, long now) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key");
        if (limit < 0) throw new IllegalArgumentException("limit");
        if (windowMillis <= 0) throw new IllegalArgumentException("window");
        if (windowMillis > maxWindow) throw new IllegalArgumentException("window exceeds maxWindow");
        if (cost <= 0) throw new IllegalArgumentException("cost");

        while (true) {
            History h = histories.computeIfAbsent(key, k -> new History());

            // one lock per key: same-key calls take turns, different keys never block each other
            synchronized (h) {
                if (h.retired) {
                    // the sweeper removed this history after we fetched it: drop it, fetch a fresh one
                    histories.remove(key, h);
                    continue;
                }

                if (now < h.lastTime) throw new IllegalArgumentException("timestamp went backwards");

                h.evict(now - maxWindow);

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
    }

    /**
     * Removes keys with no events newer than (now - maxWindow). Returns how many were removed.
     * 'now' must come from the same time source as the request timestamps; a 'now' that runs
     * ahead of a key's real timestamps discards history that was still needed.
     */
    public int sweep(long now) {
        long cutoff = now - maxWindow;
        int removed = 0;
        for (Map.Entry<String, History> e : histories.entrySet()) {
            History h = e.getValue();
            synchronized (h) {
                if (!h.retired && h.lastTime <= cutoff) {
                    h.retired = true;
                    histories.remove(e.getKey(), h);
                    removed++;
                }
            }
        }
        return removed;
    }

    /** for tests: number of keys currently held */
    int keyCount() {
        return histories.size();
    }

    /** for tests: number of events currently retained for a key */
    int liveEvents(String key) {
        History h = histories.get(key);
        if (h == null) return 0;
        synchronized (h) {
            return h.live();
        }
    }
}