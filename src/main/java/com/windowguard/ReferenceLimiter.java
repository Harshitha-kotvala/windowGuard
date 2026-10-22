package com.windowguard;
import java.util.*;
public class ReferenceLimiter implements RateLimiter{
    private record Event(long time ,int cost){

    }
    private final Map<String,List<Event>> history = new HashMap<>();

    public Decision tryAcquire(
            String key,
            int limit,
            long windowMillis,
            int cost,
            long now
    ) {
        List<Event> events = history.computeIfAbsent(key, k -> new ArrayList<>());
        long used=0;
        for(Event e:events){
            if(e.time()>now-windowMillis){
                used+=e.cost();
            }
        }
        if(used+cost<=limit){
            events.add(new Event(now,cost));
            return new Decision(true,(int) (limit-used-cost),0);
        }
        return new Decision(false, (int) Math.max(0, limit - used), 0);
    }
}