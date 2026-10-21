package com.utetra.identity;
import java.time.Clock;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
/** Bounded per-process IP limiter. Email limits also persist in SQL under the user lock. */
@Component
public class IdentityRateLimiter {
    private final Clock clock;
    private final Map<String,Deque<Long>> buckets=new HashMap<>();
    public IdentityRateLimiter(Clock clock) { this.clock=clock; }
    public synchronized void check(String key,int limit) {
        long now=clock.millis(),cutoff=now-3600000;
        buckets.values().forEach(q -> { while(!q.isEmpty() && q.peekFirst()<cutoff) q.removeFirst(); });
        buckets.entrySet().removeIf(e -> e.getValue().isEmpty());
        if(!buckets.containsKey(key) && buckets.size()>=10000) throw new IdentityException(HttpStatus.TOO_MANY_REQUESTS,"Vui lòng thử lại sau.");
        var q=buckets.computeIfAbsent(key,k -> new ArrayDeque<>());
        if(q.size()>=limit) throw new IdentityException(HttpStatus.TOO_MANY_REQUESTS,"Đã đạt giới hạn thử. Vui lòng thử lại sau.");
        q.addLast(now);
    }
}
