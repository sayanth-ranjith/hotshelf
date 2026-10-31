# Roadmap

Each iteration is small enough to finish, review and *understand* in one sitting or a few. Each one leaves the library
working and tested, with one new concept added.

## v1: The core shelf

| # | Iteration | You'll learn | Main patterns / principles |
|---|---|---|---|
| 00 | [Project foundation](iterations/00-project-foundation.md) | What makes a *library* different from an app; Maven modules | Separation of concerns, dependency hygiene |
| 01 | [The cache contract](iterations/01-cache-contract.md) | Hit vs miss, the cache API, null semantics | Program to an interface, ISP |
| 02 | [Bounded size and LRU eviction](iterations/02-lru-eviction.md) | Why caches must forget; O(1) LRU with HashMap + doubly linked list | **Strategy**, **Builder**, OCP |
| 03 | [TTL expiry](iterations/03-ttl-expiry.md) | Staleness, lazy expiry, testable time | DIP (`Ticker`), SRP |
| 04 | [Thread safety](iterations/04-thread-safety.md) | Race conditions, locks, why an LRU *read* is a *write* | **Decorator**, SRP |
| 05 | [Statistics](iterations/05-statistics.md) | Hit rate, eviction count, measuring a cache | **Null Object**, immutable value object, DIP |
| 06 | [Loading cache and stampede protection](iterations/06-loading-cache.md) | Cache-aside, thundering herd, single-flight | Single-flight, functional interfaces |
| 07 | [v1 release](iterations/07-v1-release.md) | Benchmarking with JMH, API review, versioning | Facade, API minimalism |

## v2: Smarter eviction (outline)

| # | Iteration | Ideas |
|---|---|---|
| 08 | FIFO and LFU policies | New `EvictionPolicy` implementations. This tests whether the iteration 02 design really is open for extension (OCP). O(1) LFU with frequency buckets. |
| 09 | Removal listeners | **Observer** pattern. `RemovalCause` enum (EXPLICIT, REPLACED, SIZE, EXPIRED). Calling listeners *outside* the lock. |
| 10 | Background expiry sweeper | `ScheduledExecutorService`, daemon threads, lifecycle (`AutoCloseable`), lazy vs active expiry trade-offs |
| 11 | Segmented (striped) locking | Composing N locked segments by key hash; why per-segment LRU is only *approximately* LRU; benchmarking contention |

## v3: Spring Boot integration (outline)

| # | Iteration | Ideas |
|---|---|---|
| 12 | Starter module and auto-configuration | `hotshelf-spring-boot-starter`, `@AutoConfiguration`, `@ConfigurationProperties`, conditional beans |
| 13 | Spring Cache abstraction | **Adapter** pattern: implement Spring's `CacheManager` / `Cache` on top of hotshelf so `@Cacheable`, `@CachePut` and `@CacheEvict` work |
| 14 | Micrometer metrics | Exposing `CacheStats` as meters, and why stats were designed as a snapshot value object back in iteration 05 |

## v4: Two-level cache (outline)

| # | Iteration | Ideas |
|---|---|---|
| 15 | L1 (hotshelf) + L2 (Redis) | Read-through L1 → L2 → source; **Composite** / **Chain of Responsibility**; serialization |
| 16 | Cross-instance invalidation | Redis pub/sub, eventual consistency, and why "cache invalidation is hard" |

## Always

- JMH benchmarks against `ConcurrentHashMap` and Caffeine whenever performance-relevant code changes.
- A learning log at the bottom of each iteration doc.
