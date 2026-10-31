# Iteration 03: TTL expiry

## Goal
Entries can expire a fixed time after they were written (`expireAfterWrite`). Expired entries are never returned.

```java
Hotshelf.<String, User>builder()
        .maximumSize(1_000)
        .expireAfterWrite(Duration.ofMinutes(10))
        .build();
```

## Concepts you'll learn
- **Staleness.** The source of truth changes and the cache doesn't know. TTL bounds *how* stale data can get.
- **Lazy expiry**: check the timestamp on read and treat an expired entry as a miss. It's cheap and needs no background threads,
  but expired entries keep using memory until they are touched or evicted. Redis uses lazy expiry plus periodic sampling.
  We add active sweeping in iteration 10.
- **Testable time**, and why `System.nanoTime()` beats `currentTimeMillis()` for measuring durations (ADR-004).

## Scope
**In**
- `Ticker` interface + `Ticker.system()`; a `FakeTicker` in test sources with `advance(Duration)`.
- Store values wrapped in a small `CacheEntry<V>(V value, long writeTimeNanos)` record.
- On `get`, if `now - writeTime >= ttl`, remove the entry (from store **and** policy) and return empty.
- Builder: `expireAfterWrite(Duration)` and a package-private or test-visible `ticker(Ticker)`.

**Out**
- `expireAfterAccess` (sliding expiry). It's a good stretch exercise once this works.
- Background cleanup (iteration 10).

## Design

| Decision | Pattern / principle | Why |
|---|---|---|
| `Ticker` injected into `LocalCache` | **DIP** | High-level policy ("is this expired?") doesn't depend on a low-level detail (the system clock). It also makes tests instant. |
| `CacheEntry` record holds value and metadata | **Immutable value object** | A new entry on every write; nothing to mutate by mistake |
| Expiry check in one private method `isExpired(entry, now)` | **SRP** within the class | One place to change when `expireAfterAccess` arrives |

**Open question to decide during the iteration:** what should `size()` return when expired-but-not-yet-removed
entries exist? Options: (a) count them (cheap, slightly surprising), (b) scan and purge (O(n)). Caffeine documents its
`estimatedSize()` as approximate. Write down the choice in the Javadoc and in the learning log.

## Definition of done
- [ ] Entry is returned just before TTL and absent at TTL. Tests use `FakeTicker`, with no sleeps.
- [ ] Overwriting a key resets its TTL.
- [ ] An expired entry is removed from the eviction policy too.
- [ ] No TTL configured → entries never expire, and there's no overhead beyond storing the timestamp.
- [ ] Builder rejects zero or negative durations.

## Pitfalls
- Comparing nanoTime values with `a < b` instead of `a - b < 0`. `nanoTime` can overflow, and only differences are meaningful.
- Calling `recordAccess` for an entry that turns out to be expired.

## Learning log
*(fill in after the iteration)*
