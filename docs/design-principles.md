# Design principles

This page connects textbook ideas to real hotshelf code. Every pattern listed here earns its place by solving a
concrete problem in the library. A pattern used "because it's a best practice" makes code harder to read, not better.

---

## SOLID, applied to hotshelf

### S: Single Responsibility Principle
*A class should have one reason to change.*

A cache does several distinct jobs: **store** entries, **decide what to evict**, **decide what has expired**,
**keep it thread-safe**, and **count hits and misses**. If one class did all of them, a change to locking could
break eviction. So each job gets its own home:

| Job | Owner |
|---|---|
| Store and look up entries | `LocalCache` (single-threaded core) |
| Pick an eviction victim | `EvictionPolicy` implementations (`LruEvictionPolicy`, ...) |
| Know the current time | `Ticker` |
| Thread safety | `ConcurrentCache` decorator |
| Counting | `StatsCounter` |
| Assembling all of the above | `HotshelfBuilder` |

### O: Open/Closed Principle
*Open for extension, closed for modification.*

Adding LFU in v2 should mean **adding** a class `LfuEvictionPolicy`, not editing `LocalCache` with a new `if (policy == LFU)`
branch. The `EvictionPolicy` interface (iteration 02) is what makes this possible. Iteration 08 is our test of whether we got it right.

### L: Liskov Substitution Principle
*Any implementation must be usable wherever its interface is expected, without surprises.*

Every `Cache<K, V>` must honour the same contract: `get` after `put` returns the value (unless it was evicted or expired),
`null` is always rejected, and `size()` never goes above `maximumSize` after a write completes. We enforce this with a
**shared contract test suite** (an abstract JUnit test class that runs against every implementation). That way the
`ConcurrentCache` decorator is checked against exactly the same expectations as `LocalCache`.

### I: Interface Segregation Principle
*Clients shouldn't depend on methods they don't use.*

- `EvictionPolicy` only knows about **keys**: it never sees values, so it can't accidentally depend on them.
- `StatsCounter` (written to by the cache) is separate from `CacheStats` (read by users). The writer interface and the
  read-only snapshot have different clients.
- We avoid one giant `Cache` interface with 30 methods. v1 has ~7.

### D: Dependency Inversion Principle
*Depend on abstractions, not concretions.*

`LocalCache` depends on `EvictionPolicy`, `Ticker` and `StatsCounter` **interfaces**, which are passed in through the
constructor. It never calls `new LruEvictionPolicy()` or `System.nanoTime()` itself. The payoff is **testability**:
a `FakeTicker` lets a test "wait 10 minutes" in a microsecond, which is how we test TTL without `Thread.sleep`.

---

## Design pattern catalogue

| Pattern | Where | Problem it solves | Iteration |
|---|---|---|---|
| **Program to an interface** | `Cache<K, V>` | Users and the Spring adapter depend on the contract, not on `LocalCache` | 01 |
| **Strategy** | `EvictionPolicy` | Swap LRU / LFU / FIFO without touching the store | 02 |
| **Builder** | `Hotshelf.builder()` | Many optional settings; avoid telescoping constructors; validate once in `build()` | 02 |
| **Static factory / Facade** | `Hotshelf` | One discoverable entry point; hides package-private implementation classes | 02, 07 |
| **Dependency injection (constructor)** | `LocalCache(policy, ticker, stats)` | Testability, DIP | 02–05 |
| **Decorator** | `ConcurrentCache` wraps `LocalCache` | Add thread safety without touching or duplicating the core logic | 04 |
| **Null Object** | `StatsCounter.disabled()` | "Stats off" without `if (stats != null)` checks scattered through the code | 05 |
| **Immutable value object** | `CacheStats` record | Safe to share across threads; a consistent snapshot | 05 |
| **Single-flight** (concurrency pattern) | `get(key, loader)` | Only one thread loads a missing key; the others wait for its result | 06 |
| **Observer** | Removal listeners | Notify interested code about evictions without coupling the cache to it | 09 |
| **Adapter** | Spring `Cache` / `CacheManager` | Make hotshelf speak Spring's caching interfaces | 13 |
| **Composite / Chain of Responsibility** | L1 + L2 cache | Treat a two-level cache as a single `Cache` | 15 |

### Why Decorator for thread safety (and not locks inside `LocalCache`)?
Because of SRP: `LocalCache` stays a simple, single-threaded, easy-to-test data structure, and `ConcurrentCache` holds
**only** concurrency logic. `java.util.Collections.synchronizedMap` uses the same idea. In v2 we can swap the coarse-lock
decorator for a segmented one without touching `LocalCache` at all.

### Why Strategy for eviction (and not subclasses like `LruCache extends AbstractCache`)?
Inheritance would hard-wire one policy per class. Combining it with expiry, locking and stats would then need a subclass for every
combination (`LruTtlConcurrentCache`...). Composition lets the builder **mix and match** at runtime.

---

## Patterns we deliberately avoid

- **Singleton cache.** Global state makes tests interfere with each other. Users create as many caches as they need.
- **Deep inheritance hierarchies.** See above. We favour composition.
- **Abstract base classes "for reuse".** Shared behaviour goes in a collaborator object, not a superclass.
- **Speculative generality.** No interface gets a second method "in case we need it". We add it when an iteration needs it.

---

## General rules of thumb

- Immutable where possible (`record`s for config and stats).
- Fail fast: validate builder arguments in `build()` and throw `IllegalArgumentException` with a clear message.
- Package-private by default; public only when it is part of the API.
- Each lock documents what it guards.
