# Learning map: building WindowGuard

A study path for building an in-memory sliding-window rate limiter in Java.
Each block says why you need it, what to read, and how to check you understood it.
Learn a block right before the step that needs it.

| # | Block | Needed for |
|---|-------|-----------|
| 1 | Rate limiting algorithms | Choosing and justifying the design |
| 2 | Binary search + prefix sums | Fast per-key lookup |
| 3 | Differential testing | Proving the fast version is correct |
| 4 | Memory retention | Bounded history |
| 5 | Concurrency | Thread safety and safe cleanup |
| 6 | Benchmarking | Honest performance numbers |
| 7 | Scaling beyond one JVM | Discussion only, nothing built |

## 1. Rate limiting algorithms
Why: you must be able to say why a sliding-window log was chosen over the alternatives.
- Redis rate limiting guide: https://redis.io/learn/howtos/ratelimiting/
- Redis rate limiter use case: https://redis.io/docs/latest/develop/use-cases/rate-limiter/
- Token bucket: https://en.wikipedia.org/wiki/Token_bucket
- Leaky bucket: https://en.wikipedia.org/wiki/Leaky_bucket
- Book chapter: "Design a Rate Limiter", Alex Xu, *System Design Interview* (no link, it's a book)

Cover: fixed window, sliding window log, sliding window counter, token bucket.
Check: explain in one sentence each why the log is exact but memory-heavy and counters are cheap but approximate.

## 2. Binary search and prefix sums
Why: each key's timestamps are sorted, so the start of the window can be found in O(log n), and a cumulative-cost array gives the used quota with one subtraction.
- Binary search (lower and upper bound): https://cp-algorithms.com/num_methods/binary_search.html
- Java Arrays (copyOf, binarySearch): https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/Arrays.html
- Big-O cheat sheet: https://www.bigocheatsheet.com/

Check: dry-run one request by hand, finding the first index with time > now - window and computing used from two cumulative values.

## 3. Differential testing
Why: a fast, clever version is only trustworthy if it matches a simple, obviously correct one on thousands of random inputs.
- JUnit 5 user guide: https://docs.junit.org/5.13.1/user-guide/
- Differential testing (concept): https://en.wikipedia.org/wiki/Differential_testing
- Optional, property-based testing: https://jqwik.net/docs/current/user-guide.html

Check: deliberately break the fast version (change `>` to `>=`) and confirm the test fails.

## 4. Memory retention
Why: per-call windows mean you can't delete history by the current window; you need a maximum retained window and cleanup of idle keys.
- Java collections framework: https://dev.java/learn/api/collections-framework/
- HashMap: https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/HashMap.html

Cover: why history can't be trimmed to the current window, a maxWindow retention policy, array compaction, amortized O(1) appends.
Check: explain why a window larger than maxWindow must be rejected.

## 5. Concurrency
Why: check-then-act is a race; two threads can both see room and both be allowed.
- Java concurrency tutorial: https://docs.oracle.com/javase/tutorial/essential/concurrency/
- Synchronization: https://docs.oracle.com/javase/tutorial/essential/concurrency/sync.html
- ConcurrentHashMap: https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html
- Race condition (concept): https://en.wikipedia.org/wiki/Race_condition

Cover: per-key locking, atomic check-and-record, and the race between a cleanup sweep and a request.
Check: a 100-thread test on one key never exceeds the limit.

## 6. Benchmarking
Why: JVM timing without warm-up is misleading; report only what you measured.
- JMH: https://openjdk.org/projects/code-tools/jmh/
- HdrHistogram (latency percentiles): https://github.com/HdrHistogram/HdrHistogram
- Java Flight Recorder: https://docs.oracle.com/en/java/javase/26/jfapi/flight-recorder-configurations.html

Cover: warm-up, throughput, p50/p95/p99, performance as history grows, memory per key.
Check: a results table you generated on your own machine, with the JDK and hardware noted.

## 7. Scaling beyond one JVM (reading only)
Why: the single-process version undercounts when one client hits several servers.
- Myntra golimit (open source, local + global counters, async sync): https://github.com/myntra/golimit
- Redis sorted sets: https://redis.io/docs/latest/develop/data-types/sorted-sets/
- Redis Lua scripting: https://redis.io/docs/latest/develop/programmability/eval-intro/

Cover: sticky routing vs. shared store vs. local counters with periodic sync; fail-open vs. fail-closed; clock skew; hot keys.
Check: explain the trade-offs between exact and approximate counting without notes.

## Order
1 → 2 → 3 → build the fast version → 4 → 5 → 6, with 7 as background reading.
