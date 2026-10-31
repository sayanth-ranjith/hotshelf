# Iteration 06: Loading cache and stampede protection

## Goal
`get(key, loader)` returns the cached value, or calls `loader` on a miss, stores the result and returns it.
If 100 threads miss the same key at once, **the loader runs exactly once**.

```java
User u = users.get("42", id -> userRepository.findById(id));
```

## Concepts you'll learn
- **Cache-aside** vs **read-through**. Without this method, every caller writes "get, if empty then load, then put" by hand.
  With it, the cache owns that flow, which is read-through.
- **Cache stampede (thundering herd).** A popular key expires and 500 requests miss at once, so 500 identical DB queries
  hit the database at its worst moment. This is a real production outage pattern.
- **Single-flight.** The first thread to miss starts the load. Later threads *wait for that same result* instead of starting their own.
- `CompletableFuture` as a "promise of a value" that many threads can wait on.
- Why you must **not** hold the cache lock while loading. A slow DB call would freeze every other key.

## Scope
**In**
- `V get(K key, Function<? super K, ? extends V> loader)` added to `Cache`.
- An in-flight map `ConcurrentHashMap<K, CompletableFuture<V>>` in `ConcurrentCache`:
  1. Under the lock, check the cache. On a hit, return it.
  2. Otherwise `inFlight.computeIfAbsent(key, ...)`. The winner creates the future and runs the loader **outside** the cache lock.
  3. The winner `put`s the result, completes the future and removes the in-flight entry.
  4. The others call `future.join()`.
- Loader returning `null` → `NullPointerException` (consistent with ADR-002). A loader that throws → the exception propagates to *all*
  waiters, and nothing is cached.

**Out**
- Async API (`CompletableFuture<V> getAsync`). It's a possible stretch goal.
- Refresh-ahead (reloading before expiry). Note it for later.

## Design

| Decision | Pattern / principle | Why |
|---|---|---|
| In-flight futures keyed by cache key | **Single-flight** (concurrency pattern) | Deduplicates concurrent loads per key, while different keys still load in parallel |
| Loader is a `Function<K, V>` | Functional interface / **Strategy** (lightweight) | The caller supplies "how to load". The cache supplies "when". |
| Lives in `ConcurrentCache`, not `LocalCache` | **SRP** | Coordinating threads is a concurrency concern. Single-threaded `LocalCache` can implement `get(key, loader)` trivially. |

**Rejected:** loading while holding the cache lock. It's simple and correct, but one slow load blocks *every* key, which is unacceptable.
**Rejected:** a lock per key. It works, but those locks need cleanup to avoid leaking memory. Futures are removed naturally when the load completes, and they also carry the result or the exception.

## Definition of done
- [ ] Hit → loader never called.
- [ ] 50 threads + start latch + a loader that sleeps briefly and counts invocations → count == 1, all threads get the same value.
- [ ] Two different keys load concurrently. One slow key doesn't block the other.
- [ ] Loader exception → every waiter sees it, nothing is cached, and the next call retries.
- [ ] Stats: a load counts as a miss.

## Pitfalls
- Forgetting to remove the in-flight entry when the loader throws, so the key is "loading" forever. Use `finally`.
- A loader that calls `get` on the same cache for the same key causes a deadlock. Document it like Caffeine does.

## Learning log
*(fill in after the iteration)*
