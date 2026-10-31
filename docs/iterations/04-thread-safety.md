# Iteration 04: Thread safety

## Goal
Caches built by `Hotshelf.builder()` are safe to use from many threads at once.

## Concepts you'll learn
- **Race conditions.** Two threads doing `put` at once can corrupt the linked list, for example by leaving a node
  pointing at an unlinked neighbour, or exceed `maximumSize`.
- **Atomicity.** "Check size, then evict, then insert" must happen as one indivisible step.
- **Why an LRU read is a write.** `get` reorders the list, so a `ReadWriteLock` buys nothing here (ADR-003). This is the
  single most important insight of the iteration.
- **Visibility and happens-before.** A lock doesn't only exclude other threads. It also guarantees each thread sees the previous holder's writes.
- How to *test* concurrency: many threads, a `CountDownLatch` start gate, then check the invariants.

## Scope
**In**
- `ConcurrentCache<K, V> implements Cache<K, V>`: wraps any `Cache` and guards every call with one `ReentrantLock`.
- `LocalCache` stays completely unsynchronised. Its Javadoc says "not thread-safe".
- Builder always returns `new ConcurrentCache<>(new LocalCache<>(...))`.
- Multi-threaded stress tests.

**Out**
- Finer-grained (segmented) locking (iteration 11).

## Design

```
Cache ◄── ConcurrentCache (lock) ──wraps──► LocalCache (store + policy + ticker)
```

```java
final class ConcurrentCache<K, V> implements Cache<K, V> {
    private final Cache<K, V> delegate;          // guarded by lock
    private final ReentrantLock lock = new ReentrantLock();

    public Optional<V> get(K key) {
        lock.lock();
        try { return delegate.get(key); }
        finally { lock.unlock(); }
    }
    ...
}
```

| Decision | Pattern / principle | Why |
|---|---|---|
| Locking in a wrapper with the same interface | **Decorator** | Adds a responsibility (thread safety) without modifying `LocalCache`, the same idea as `Collections.synchronizedMap`. |
| `LocalCache` stays single-threaded | **SRP** | Data structure logic is tested without thread noise, and concurrency logic is reviewed in one small class. |
| `ReentrantLock` over `synchronized` | Explicit API | Same semantics here, but it supports `tryLock` and timeouts and reads more clearly in a learning project. Either is acceptable, and we note that. |
| Exclusive lock, not `ReentrantReadWriteLock` | Correctness first | LRU `get` mutates. See ADR-003. |

**Rejected:** `ConcurrentHashMap` as the store. It makes the *map* thread-safe, but the map plus the linked list together still
need to change atomically, so we'd need a lock anyway.

## Definition of done
- [ ] `ConcurrentCache` passes the `CacheContractTest` suite.
- [ ] Stress test: N threads × M random `put`, `get` and `remove` calls; afterwards `size() <= maximumSize` and the LRU
      list's length equals the store's size (add a package-private consistency check for tests).
- [ ] Every lock acquisition is paired with `unlock()` in a `finally` block.

## Pitfalls
- Locking `get` but forgetting `size()` or `clear()`.
- Stress tests that "pass" because threads never actually overlapped. Use a start latch so they all begin together.

## Learning log
*(fill in after the iteration, including roughly how much slower the locked cache is in a quick test)*
