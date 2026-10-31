# Iteration 05: Statistics

## Goal
Optionally record hits, misses and evictions, and expose them as a consistent snapshot.

```java
Cache<String, User> users = Hotshelf.<String, User>builder().maximumSize(1_000).recordStats().build();
CacheStats s = users.stats();   // hitCount, missCount, hitRate(), evictionCount
```

## Concepts you'll learn
- **Hit rate** = hits / (hits + misses): the single most important number for judging a cache. A cache with a 5% hit rate is
  mostly overhead.
- Why stats are **opt-in**: counting costs a little on every operation.
- Snapshot vs live view: why `stats()` returns an immutable copy.

## Scope
**In**
- `CacheStats` public `record (long hitCount, long missCount, long evictionCount)` with derived `hitRate()`,
  `requestCount()` and `minus(other)` for "stats over an interval".
- `StatsCounter` package-private interface: `recordHit()`, `recordMiss()`, `recordEviction()`, `snapshot()`.
- `ConcurrentStatsCounter` using `LongAdder`, and `StatsCounter.disabled()`, which does nothing and returns zeros.
- `Cache.stats()` added to the interface.
- Builder: `recordStats()`.

**Out**
- Load success, failure and timing stats (added in iteration 06 if useful).
- Micrometer (iteration 14).

## Design

| Decision | Pattern / principle | Why |
|---|---|---|
| `StatsCounter.disabled()` instead of `null` | **Null Object** | The cache always calls `stats.recordHit()`, with no `if (recordStats)` branches anywhere. Turning stats off is a builder choice, not a code path. |
| `StatsCounter` injected into `LocalCache` | **DIP** | Tests can pass a counter and assert on it directly |
| `CacheStats` is a `record` | **Immutable value object** | Safe to hand to any thread, log, or send to metrics. It can't change underneath the reader. |
| Writer interface (`StatsCounter`) separate from the read model (`CacheStats`) | **ISP** | The cache only writes; users only read. |
| `LongAdder` over `AtomicLong` | Performance | Under contention, `LongAdder` spreads increments across cells. It's a nice JDK detail to learn. |

**Where counts happen:** `LocalCache` knows whether a lookup was a hit or a miss and when it evicted, so it calls the counter.
An expired entry found on `get` counts as a **miss**, and as an eviction only if we decide expiry counts as an eviction.
Decide which, and document it.

## Definition of done
- [ ] Hit, miss and eviction counts are correct in unit tests, including expiry scenarios using `FakeTicker`.
- [ ] `hitRate()` is 1.0 when there are no requests (Caffeine's convention, which avoids divide-by-zero). Document it.
- [ ] Stats disabled → `stats()` returns all zeros, and no counter allocations happen per operation.
- [ ] Counts are exact under the concurrent stress test from iteration 04.

## Pitfalls
- Counting a `put` as a hit or miss. Only reads count.

## Learning log
*(fill in after the iteration)*
