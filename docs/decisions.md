# Architecture decision log

Short records of decisions that shape the codebase. Format: context → decision → consequences.
Add a new entry instead of rewriting an old one; mark an old entry *Superseded by ADR-NNN* if it changes.

---

## ADR-001: The core library has no Spring (or any) runtime dependency
**Status:** Accepted (iteration 00)

**Context.** The project was generated from Spring Initializr, so the single module depends on Spring Boot and has a
`main` method. A cache *library* gets embedded in other people's apps. Forcing Spring on them is a heavy, version-conflicting dependency.

**Decision.** Split into a multi-module Maven build: `hotshelf-core` (plain Java 21, zero runtime dependencies) and,
in v3, `hotshelf-spring-boot-starter`, which depends on core. Remove `HotshelfApplication`.

**Consequences.** Core is usable anywhere. Spring concerns are isolated in one module (SRP at the module level).

---

## ADR-002: Reject `null` keys and values
**Status:** Accepted (iteration 01)

**Context.** If `get` returns `null`, does that mean "absent" or "cached null"? `HashMap` allows both, and the ambiguity causes bugs.

**Decision.** `put(null, ...)` and `put(..., null)` throw `NullPointerException`. `get` returns `Optional<V>`.
This matches `ConcurrentHashMap` and Caffeine.

**Consequences.** Absence is always explicit. Users who want to cache "not found" wrap it, e.g. `Optional<User>` as the value type.

---

## ADR-003: Start with a single exclusive lock, not `ReentrantReadWriteLock`
**Status:** Accepted (iteration 04)

**Context.** The README originally planned `ReentrantReadWriteLock`. But in an LRU cache, `get` **moves the entry to
the front of the list**, so it modifies shared state and needs the *write* lock anyway. A read/write lock would only
add overhead.

**Decision.** v1 uses one `ReentrantLock` in the `ConcurrentCache` decorator. v2 (iteration 11) improves concurrency by
splitting the cache into segments instead.

**Consequences.** Simple and obviously correct. Throughput under heavy contention is limited, and the JMH baseline in iteration 07 will show by how much.

---

## ADR-004: Time comes from a `Ticker` abstraction
**Status:** Accepted (iteration 03)

**Context.** TTL logic needs the current time. Calling `System.nanoTime()` directly makes tests slow (`Thread.sleep`) and flaky.

**Decision.** `interface Ticker { long read(); }` returns nanoseconds. Production uses `Ticker.system()` and tests use a `FakeTicker`.
Nanoseconds from `System.nanoTime()` are used rather than `System.currentTimeMillis()`, because nanoTime is monotonic
and is not affected by wall-clock changes such as NTP sync or daylight saving.

**Consequences.** TTL tests are instant and deterministic.
