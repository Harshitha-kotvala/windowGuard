# WindowGuard

WindowGuard is a small Java library for exact, per-key sliding-window rate
limiting. It supports weighted requests and reports both remaining quota and
how long a rejected request should wait before retrying.

## Requirements

- Java 21+
- Maven

## Add it to your project

WindowGuard is not currently published to a Maven repository. To use it, build
the project and add the resulting JAR to your application, or include the
source in your project.

Build the JAR with:

```shell
mvn package
```

## Quick start

```java
import com.windowguard.Decision;
import com.windowguard.FastLimiter;
import com.windowguard.RateLimiter;

class Example {
    public static void main(String[] args) {
        // Retain enough history to serve windows up to 60 seconds.
        RateLimiter limiter = new FastLimiter(60_000);

        long now = System.currentTimeMillis();
        Decision decision = limiter.tryAcquire(
                "customer-123",
                100,       // maximum cost in the window
                60_000,    // window length in milliseconds
                1,         // cost of this request
                now
        );

        if (decision.allowed()) {
            System.out.println("Allowed; remaining quota: " + decision.remaining());
        } else if (decision.retryAfterMillis() == Decision.NEVER) {
            System.out.println("This request can never fit within the configured limit.");
        } else {
            System.out.println("Rejected; retry after "
                    + decision.retryAfterMillis() + " ms.");
        }
    }
}
```

Use a stable key for each client or other identity whose usage should be
counted independently. The caller supplies the current time in milliseconds;
timestamps for a key must not go backwards.

## Behavior

- Each key has an independent quota.
- A request is allowed when its cost plus the cost of accepted requests still
  inside the window is no greater than the limit.
- The window is `(now - windowMillis, now]`: an event exactly at the left
  boundary has expired.
- Rejected requests do not consume quota.
- `remaining` is the available quota after the decision; it is zero when the
  current usage already meets or exceeds the limit.
- For a rejection, `retryAfterMillis` is the earliest delay until enough
  recorded cost expires for that request to fit, assuming no intervening
  requests. It is `Decision.NEVER` when the request cost itself exceeds the
  limit.
- Keys must be non-null and non-blank; limits must be non-negative; request
  costs and window lengths must be positive.
- `FastLimiter` requires a positive `maxWindow`, and rejects requests for
  windows longer than it. That bound lets it evict history that cannot be used
  by any supported window.

## Implementations

| Class | Purpose |
| --- | --- |
| `FastLimiter` | Optimized implementation using per-key timestamp and cumulative-cost arrays with binary search. Configure its maximum supported window at construction. |
| `ReferenceLimiter` | Simple implementation that scans request history. Useful as a readable reference and correctness oracle; it does not impose a maximum window. |

Both implementations are in the `com.windowguard` package and implement
`RateLimiter`. `FastLimiter` currently uses mutable in-memory state and is not
thread-safe; use it from a single thread unless synchronization is provided by
the caller.

## Run the tests

```shell
mvn test
```

The tests cover window boundaries, validation, retry timing, and randomized
differential comparisons between `FastLimiter` and `ReferenceLimiter`.

## Current scope

WindowGuard is an in-memory, single-process library. It does not currently
provide distributed quota coordination, idle-key cleanup, or built-in
thread-safety.
