# 🔥 hotshelf

> **Hot data, within arm's reach.**
> A lightweight, thread-safe caching library for Java and Spring Boot, built from scratch to understand how caches really work.

[![CI](https://github.com/sayanth-ranjith/hotshelf/actions/workflows/ci.yml/badge.svg)](https://github.com/sayanth-ranjith/hotshelf/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Status](https://img.shields.io/badge/status-work%20in%20progress-yellow)

---

## Why hotshelf?

Every backend eventually asks the same question: *why are we hitting the database again for data we fetched a second ago?*

A cache keeps frequently used data on a "hot shelf" in memory, so the next read is a quick reach instead of a trip to the warehouse (your database, API, or mainframe).

hotshelf is a learn-by-building project: every feature is implemented by hand, explained, tested, and benchmarked against production caches like Caffeine.

---

## The idea in one picture

```
         get("user:42")
               │
               ▼
      ┌─────────────────┐   hit    ┌──────────────┐
      │    hotshelf     │ ───────▶ │ return value │   ~ nanoseconds
      └─────────────────┘          └──────────────┘
               │ miss
               ▼
      ┌─────────────────┐
      │  DB / API call  │  ~ milliseconds
      └─────────────────┘
               │
               ▼
     put on the shelf, then return
```

---

## Planned API (v1)

```java
Cache<String, User> users = Hotshelf.<String, User>builder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(10))
        .evictionPolicy(EvictionPolicy.LRU)
        .recordStats()
        .build();

users.put("42", user);
Optional<User> cached = users.get("42");

// Load on miss: only one thread hits the DB for a missing key
User u = users.get("42", id -> userRepository.findById(id));

CacheStats stats = users.stats();   // hits, misses, hitRate, evictions
```

> ⚠️ This API is a design target. It is being built step by step; check the roadmap for what's done.

---

## Roadmap

> 📚 The step-by-step plan, with the design rationale for each step, lives in [`docs/`](docs/README.md). Start with [`docs/roadmap.md`](docs/roadmap.md).

### v1: The core shelf
- [ ] `Cache<K, V>` interface: `get`, `put`, `remove`, `clear`, `size`
- [ ] **LRU eviction** in O(1) using a `HashMap` plus a doubly linked list
- [ ] **Max size** limit
- [ ] **TTL expiry** (`expireAfterWrite`), lazy on read
- [ ] **Thread safety** with a lock-based decorator (and why a read/write lock doesn't help LRU, see [ADR-003](docs/decisions.md))
- [ ] **Loading cache**: `get(key, loader)` with stampede protection
- [ ] **Stats**: hits, misses, hit rate, evictions
- [ ] Fluent **builder**
- [ ] JUnit 5 test suite

### v2: Smarter eviction
- [ ] LFU and FIFO policies behind an `EvictionPolicy` interface
- [ ] Background expiry sweeper
- [ ] Eviction listeners
- [ ] Segmented locking for higher concurrency

### v3: Spring Boot integration
- [ ] `hotshelf-spring-boot-starter` with auto-configuration
- [ ] Works with `@Cacheable`, `@CacheEvict`, `@CachePut`
- [ ] Configuration via `application.yml`
- [ ] Micrometer metrics

### v4: Two-level cache
- [ ] L1 hotshelf (in-memory) plus L2 Redis
- [ ] Cross-instance invalidation with Redis pub/sub

### Always
- [ ] JMH benchmarks vs Caffeine and `ConcurrentHashMap`
- [ ] A short write-up per milestone explaining the "why"

---

## Concepts covered

| Concept | Where it shows up |
|---|---|
| Hit / miss / hit rate | Stats |
| Eviction (LRU, LFU, FIFO) | Choosing what to drop when the shelf is full |
| TTL / expiry | Stale data protection |
| Cache stampede | Loading cache |
| Concurrency and locking | Thread-safe core |
| Cache-aside pattern | Spring integration |
| Cache invalidation | Two-level cache |

> *"There are only two hard things in Computer Science: cache invalidation and naming things."* We already handled one of them. 😉

---

## Tech stack

- Java 21
- Maven
- JUnit 5
- Spring Boot 3 (from v3)
- JMH (benchmarks)

---

## License

[MIT](LICENSE)

---

## Author

Built by [Sayanth](https://github.com/sayanth-ranjith), a backend developer learning caching from the ground up.
