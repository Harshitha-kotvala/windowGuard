# Learning Roadmap

## 1. Java Fundamentals

- [Java Collections Framework](https://dev.java/learn/api/collections-framework/)
- [HashMap](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/HashMap.html)
- [ArrayDeque](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/ArrayDeque.html)
- [Arrays](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/Arrays.html)
- [Java Records](https://dev.java/learn/records/)
- [Exception Handling](https://dev.java/learn/exceptions/)

Topics:
- Collections and their internal behavior
- Arrays, resizing and copying
- Hashing and collision handling
- Time and space complexity
- Generics and interfaces
- Records and immutability
- Input validation and exception handling

## 2. Data Structures and Algorithms

- [Binary Search](https://cp-algorithms.com/num_methods/binary_search.html)
- [Big-O Cheat Sheet](https://www.bigocheatsheet.com/)

Topics:
- Lower bound and upper bound
- Searching sorted arrays
- Sliding window and two-pointer techniques
- Queue and deque operations
- Amortized analysis
- Array compaction and resizing
- Time-based data retention

## 3. Rate Limiting

- [Redis Rate Limiting Guide](https://redis.io/learn/howtos/ratelimiting/)
- [Redis Rate Limiter Use Case](https://redis.io/docs/latest/develop/use-cases/rate-limiter/)

Topics:
- Fixed window counter
- Sliding window log
- Sliding window counter
- Token bucket
- Leaky bucket
- Request quotas and admission control
- Window boundaries and expiration
- Variable limits and window sizes
- Request costs and retry-after calculation
- Exact versus approximate rate limiting

## 4. Concurrency and Thread Safety

- [Java Concurrency Tutorial](https://docs.oracle.com/javase/tutorial/essential/concurrency/)
- [Synchronization](https://docs.oracle.com/javase/tutorial/essential/concurrency/sync.html)
- [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html)

Topics:
- Threads and shared mutable state
- Race conditions
- Critical sections and synchronization
- Atomicity and thread safety
- Per-key locking
- Concurrent collections
- Safe cleanup of shared state

## 5. Memory Management

- [Java Learning Resources](https://dev.java/learn/)
- [Java Flight Recorder](https://docs.oracle.com/en/java/javase/26/jfapi/flight-recorder-configurations.html)

Topics:
- Heap memory and garbage collection
- Object allocation and memory overhead
- Bounded data structures
- Stale entry removal
- Idle-key expiration
- Memory leaks and object retention
- Array capacity and compaction

## 6. Testing

- [JUnit 5 User Guide](https://docs.junit.org/5.13.1/user-guide/)
- [jqwik User Guide](https://jqwik.net/docs/current/user-guide.html)

Topics:
- Unit and parameterized testing
- Boundary and edge-case testing
- Reference implementations
- Randomized testing
- Property-based testing
- Differential testing
- Concurrent request testing
- Cleanup and memory tests

## 7. Benchmarking and Performance

- [Java Microbenchmark Harness (JMH)](https://openjdk.org/projects/code-tools/jmh/)
- [HdrHistogram](https://github.com/HdrHistogram/HdrHistogram)

Topics:
- Benchmark setup and warm-up
- Throughput and latency
- p50, p95 and p99 latency
- Performance at different history sizes
- Memory usage per client
- Garbage collection overhead
- Benchmark reproducibility

## 8. Optional: Redis

- [Redis Data Types](https://redis.io/docs/latest/develop/data-types/)
- [Redis Sorted Sets](https://redis.io/docs/latest/develop/data-types/sorted-sets/)
- [Redis Lua Scripting](https://redis.io/docs/latest/develop/programmability/eval-intro/)

Topics:
- Sorted sets and timestamp indexing
- Key expiration
- Atomic check-and-update operations
- Lua scripts
- Concurrent access across instances
- Network latency and shared-state trade-offs

## Implementation Order

1. Java collections and complexity analysis
2. Sliding-window algorithms and time semantics
3. In-memory implementation
4. Concurrency and memory management
5. Unit and randomized testing
6. Benchmarking and documentation
7. Redis extension, if needed