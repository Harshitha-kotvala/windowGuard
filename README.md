# WindowGuard

A rate limiter for Java applications. For each incoming request it answers one question:
*has this client used up its quota in the last N milliseconds?* The limit, the time window
and the cost of a request can be different on every call, so one client can be checked
against several policies (for example, 5 OTPs per 10 minutes and 3 promotional messages
per day).

It is an exact sliding-window implementation that runs inside a single Java process, with
bounded memory, safe use from many threads, and a test suite that checks the optimized
version against a deliberately simple one.

## Results at a glance

- **Correct:** the optimized limiter returns the same decision as a simple reference
  implementation on thousands of randomly generated request sequences, including changing
  limits, windows and costs.
- **Fast:** about 45 ns per decision on one thread, and that stays flat as the number of
  stored requests per client grows from 100 to 10,000 (the simple version slows down
  linearly: 311 ns, 966 ns, 9,317 ns).
- **Bounded memory:** about 313 bytes per client with 5 stored requests. Old history and idle
  clients are removed, so memory does not grow forever.
- **Thread-safe:** 100 threads hitting one client never admit more than its limit
  (200 repeated rounds). Different clients never block each other.

## How to use it

```java
FastLimiter limiter = new FastLimiter(60_000);   // largest window the limiter will accept

Decision d = limiter.tryAcquire("user-123", 5, 10_000, 1, nowMillis);
//                              client      limit window  cost  time

if (d.allowed()) {
    // handle the request; d.remaining() units are left in this window
} else {
    // reject (for example with HTTP 429); try again after d.retryAfterMillis()
    // retryAfterMillis is Decision.NEVER if the request can never fit (cost > limit)
}

limiter.sweep(nowMillis);   // call periodically to release clients that went quiet
```

A request is allowed when `used + cost <= limit`, where `used` is the total cost of the
requests already accepted for that client inside the window.

## Behaviour, precisely

- The window is `(now - window, now]`. A request exactly `window` ago no longer counts.
- Rejected requests are not recorded and use no quota.
- Limit and window may change on every call, even for the same client.
- `retryAfterMillis` is exact: the time until enough older requests leave the window for this
  one to fit.
- Time is supplied by the caller and must not go backwards for a given client; requests from
  different clients can be interleaved freely. This also makes the limiter fully
  deterministic to test.
- Invalid input (blank client, negative limit, non-positive window or cost, a window larger
  than the configured maximum, a timestamp that goes backwards) is rejected with an
  `IllegalArgumentException` instead of silently corrupting a counter.

## Design decisions

**Exact log of requests instead of counters.** Each client keeps the timestamps of its
accepted requests. That gives exact answers and supports a different window on every call,
which a fixed-window counter cannot do. The cost is memory proportional to the requests inside
the window, which the next decisions keep under control.

**Binary search plus prefix sums.** A client's timestamps are already sorted because time
does not go backwards, so the start of the window is found with a binary search, O(log n),
instead of scanning. Costs are stored as running totals, so the quota used in any window is
one subtraction. A single running total would not work, because the window changes on every
call.

**Retention by a maximum window, not by the current request's window.** If a request with a
10-second window deleted older history, the next request asking for a 60-second window would
be undercounted. So history is kept up to a configured maximum window, requests asking for a
larger window are rejected, and anything older than the maximum can never matter and is
dropped. Freed array space is reused by compaction.

**One lock per client.** Checking the quota and recording the request happen as one step under
the client's own lock. Without that, two threads can both see room for one more request and
both be admitted. Different clients use different locks and proceed in parallel.

**Safe cleanup of idle clients.** `sweep` removes clients with no recent requests. A subtle
race exists here: a request can fetch a client's history just before the sweeper removes it,
then record into a history nobody can reach, losing that request. Each history carries a
"retired" flag that is set and checked under the same lock, so a request that finds a retired
history discards it and retries with a fresh one. The caller schedules `sweep`; the library
starts no background threads.

## How it is tested

- **Reference implementation.** `ReferenceLimiter` is a short list-and-loop version that never
  deletes anything. It is slow but easy to verify by reading, and it acts as the answer key.
- **Differential tests.** The optimized limiter must match the reference on every field of
  every decision (allowed, remaining, retry delay): 2,000 random sequences of 200 requests
  across several clients with varying limits, windows and costs, plus a 20,000-request run on
  one client that forces repeated array growth and compaction.
- **Concurrency tests.** 100 threads on one client over 200 rounds (never more than the limit
  admitted), 20 clients in parallel, and a sweeper running continuously against live
  requests with no accepted request lost.
- **Boundary and input tests.** Window edges, rejected requests, exact retry times,
  oversized costs, and every invalid-input rule.
- **Memory test.** After 10,000 requests on one client, only the events inside the maximum
  window are retained.
- Each test was checked by deliberately breaking the code it covers and confirming the test
  fails. 19 tests in total; run them with `mvn test`.

## Benchmark

A timing loop with warm-up (`src/test/java/com/windowguard/Bench.java`), not a full JMH suite.
One thread, one client, every request accepted, one request per time unit, window equal to the
history size. JDK 23.0.1, 12 cores. Treat the figures as indicative.

| Stored requests per client | Simple reference (ns/op) | WindowGuard (ns/op) |
|---------------------------:|-------------------------:|--------------------:|
| 100                        | 311                      | 45                  |
| 1,000                      | 966                      | 42                  |
| 10,000                     | 9,317                    | 46                  |

The reference never deletes history, so it scans every request it has ever accepted, which is
exactly the cost the optimized version removes. Memory is about 313 bytes per client at 5
stored requests (client name included, approximate).

## Scope

Runs inside one Java process, with time supplied by the caller. It is a library component
you embed in an application, not a standalone service.

## Project layout

```
src/main/java/com/windowguard/
  RateLimiter.java          interface
  Decision.java             result: allowed, remaining, retryAfterMillis
  ReferenceLimiter.java     simple, obviously-correct version used as the answer key
  FastLimiter.java          the optimized, thread-safe implementation
src/test/java/com/windowguard/
  ...Test.java              unit, differential, concurrency and cleanup tests
  Bench.java                benchmark
docs/                       requirements and design notes
```

## Build

Requires Java 21+ and Maven: `mvn test`

---
Harshitha Kotvala