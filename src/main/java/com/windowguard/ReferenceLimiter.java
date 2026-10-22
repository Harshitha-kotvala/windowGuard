package com.windowguard;

import java.util.*;

public class ReferenceLimiter implements RateLimiter {

    private record Event(long time, int cost) {}

    private final Map<String, List<Event>> history = new HashMap<>();

    @Override
    public Decision tryAcquire(String key, int limit, long windowMillis, int cost, long now) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("key");
        if (limit < 0) throw new IllegalArgumentException("limit");
        if (windowMillis <= 0) throw new IllegalArgumentException("window");
        if (cost <= 0) throw new IllegalArgumentException("cost");

        List<Event> events = history.computeIfAbsent(key, k -> new ArrayList<>());
        if (!events.isEmpty() && now < events.get(events.size() - 1).time())
            throw new IllegalArgumentException("timestamp went backwards");

        long used = 0;
        for (Event e : events) {
            if (e.time() > now - windowMillis) {
                used += e.cost();
            }
        }

        if (used + cost <= limit) {
            events.add(new Event(now, cost));
            return new Decision(true, (int) (limit - used - cost), 0);
        }

        // ---- rejected: everything below is new ----
        int remaining = (int) Math.max(0, limit - used);
        if (cost > limit) {
            return new Decision(false, remaining, -1);   // can never fit
        }
        long freed = 0;
        for (Event e : events) {
            if (e.time() <= now - windowMillis) continue;   // already outside the window
            freed += e.cost();
            if (used - freed + cost <= limit) {
                return new Decision(false, remaining, e.time() + windowMillis - now);
            }
        }
        return new Decision(false, remaining, -1);   // shouldn't be reachable
    }
}