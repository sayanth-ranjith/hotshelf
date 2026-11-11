# Architecture decision log

Short records of decisions that shape the codebase. Format: context → decision → consequences.
Add a new entry instead of rewriting an old one; mark an old entry *Superseded by ADR-NNN* if it changes.

---

## ADR-001: The core library has no Spring (or any) runtime dependency
**Status:** Superseded by ADR-009 (iteration 02)

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

---

## ADR-005: API package plus an unexported `internal` package
**Status:** Superseded by ADR-009 (iteration 02)

**Context.** Users should depend only on the API (`Cache`, `Hotshelf`), never on implementation classes, so we can
rewrite them in every iteration. With everything in one package, `LocalCache` can be package-private. Once the
implementation moves to its own package, `Hotshelf` can only create it if it is `public`, and then anyone could.

**Decision.**
- `io.github.sayanth_ranjith.hotshelf` holds the public API.
- `io.github.sayanth_ranjith.hotshelf.internal` holds implementations.
- `module-info.java` exports only the API package. On the module path, using `internal` is a compile error.
  On the classpath, the `internal` name signals "not API", the same convention JDK and Netty use.
- Package names are lowercase, and the GitHub name's hyphen becomes `_`, because `-` isn't allowed in Java identifiers.

**Rejected.**
- *One flat package with package-private classes.* It hides implementations perfectly, but becomes a single crowded
  folder by v2 (policies, decorators, stats, loaders).
- *`core` / `impl` names.* `core` repeats the module name (`hotshelf-core`) and says nothing about visibility.
  `internal` tells users not to touch it.

**Consequences.** API and implementation are visibly separate. `internal` classes that need cross-package access are
`public` but documented as non-API. Tests run on the module path, and Surefire patches them into the module.

---

## ADR-006: CI on GitHub Actions, with test results shown in GitHub
**Status:** Accepted (chore, after iteration 01)

**Context.** Tests only ran on the author's machine. A PR could be merged with a red build, and the only test report
was console output.

**Decision.**
- GitHub Actions runs `./mvnw verify` on every push to `main` and every PR.
- It uses a JDK matrix: 21 (the minimum we support) and 25 (latest LTS). That catches "works on my JDK" problems early.
- `mikepenz/action-junit-report` turns Surefire's `TEST-*.xml` into a GitHub check, with failure annotations, and a
  job summary listing every test. The raw XML is uploaded as an artifact.

**Rejected.**
- *External dashboards (Develocity build scans, Codecov test analytics).* Big projects such as Spring, JUnit and
  Caffeine use them. They need an account or token and add little for one module with 37 tests. Revisit at v1 release.
- *Surefire HTML report only (`maven-surefire-report-plugin`).* You'd have to download a zip to see results.
  The check plus summary shows them in the PR itself.

**Consequences.** Every PR shows a green or red check per JDK. Third-party actions are pinned by major version; pin by
commit SHA if supply-chain hardening matters later.

---

## ADR-007: Eviction code lives in `internal.eviction`
**Status:** Superseded by ADR-009 (iteration 02)

**Context.** Iteration 01 said a sub-package such as `internal.eviction` is created only once it holds several classes.
Iteration 02 adds `EvictionPolicy`, `NoEvictionPolicy` and soon `LruEvictionPolicy`, with FIFO and LFU to follow in
iteration 08. Mixed in with `LocalCache`, `internal` would turn into one flat folder of everything.

**Decision.** One package per concern under `internal`. `internal.eviction` holds the `EvictionPolicy` interface and all
its implementations side by side (flat, no `impl` sub-package). `LocalCache` stays in `internal`. Later concerns
(expiry, stats, concurrency) get their own packages when they arrive.

**Rejected.**
- *Interface in `internal.eviction`, implementations in `internal.eviction.impl`.* Java has no sub-package access, so
  every implementation would have to be `public`. The extra package adds a folder level and no protection.
- *Keep everything in `internal`.* The types could stay package-private, but the package would mix storage and policy.

**Consequences.** `EvictionPolicy` and `NoEvictionPolicy` are `public` because `LocalCache` uses them from another
package. They remain non-API: `module-info.java` does not export `internal.eviction` (ADR-005). Implementations that
`LocalCache` never creates directly can stay package-private. Tests that need a fake policy implement the public interface.

---

## ADR-008: LRU is backed by `LinkedHashMap` for now
**Status:** Accepted (iteration 02)

**Context.** The plan was to hand-write the LRU list (`HashMap<K, Node>` plus a doubly linked list with sentinels).
That is a data-structures exercise more than a caching one, and it was competing for attention with the cache
concepts this project is about (bounding, eviction, expiry, concurrency, stats, loading).

**Decision.** `LruEvictionPolicy` wraps a `LinkedHashMap<K, Boolean>` with `accessOrder = true`. Every `get` or `put`
of a present key moves it to the end, so `firstEntry()` is always the least recently used key. All operations are O(1).

**Rejected (for now).** *Hand-written doubly linked list.* Same complexity, more code, more ways to get the map and list
out of sync. Kept as an optional deep dive (iteration 02 "Later").

**Consequences.** Steps 4 to 6 of the iteration 02 plan are dropped, including the differential test, which would
only compare `LinkedHashMap` with itself. Because `LocalCache` depends only on `EvictionPolicy` (DIP), the hand-written
version can replace this one later without touching `LocalCache`.

---

## ADR-009: One plain library module on Spring Boot's build, no JPMS, packages by concern
**Status:** Accepted (iteration 02). Supersedes ADR-001, ADR-005 and ADR-007.

**Context.** The parent POM + `hotshelf-core` module, `module-info.java` and the `internal` package added ceremony
(module exports, `public`-only-for-wiring classes, two POMs) and even caused a bug: `HotshelfBuilder.evictionPolicy(...)`
took an enum from an unexported package, so module-path users couldn't call it. The author wants a normal, neat,
Spring-based Java project that is still a **library** other applications depend on.

**Decision.**
- One Maven module whose parent is `spring-boot-starter-parent`, for Spring-managed versions of plugins and test libraries.
- It builds a **plain library jar**: no `spring-boot-maven-plugin` (it would repackage the jar into an executable app
  that nobody can depend on) and no `main` class.
- **Zero runtime dependencies.** Spring is test-scoped. A Spring Boot demo app (`demo.DemoApplication`) lives in
  `src/test` and shows hotshelf used as a bean, without being shipped. When Spring auto-configuration arrives (v3),
  Spring becomes an `<optional>` dependency, so users who don't use Spring never get it.
- No `module-info.java`. No `internal` package. Packages are grouped by concern under `io.github.sayanth_ranjith.hotshelf`:
  root = API (`Cache`, `Hotshelf`, `HotshelfBuilder`), `cache` = implementations, `eviction` = policies; later `expiry`, `stats`, ...

**Rejected.**
- *A Spring Boot application with `spring-boot-starter` as a compile dependency and a `@Configuration` in main sources.*
  Tried first. The jar was repackaged (classes under `BOOT-INF/`), every user would inherit Spring Boot, and a user's
  component scan could pick up our `@Configuration` and register beans they never asked for. Libraries plug into Spring
  through auto-configuration, which users can see and override.
- *Keep the multi-module + JPMS layout and fix only the enum.* Correct, but the author wants less ceremony while learning.

**Consequences.** Classes used across packages (`LocalCache`, `NoEvictionPolicy`, `EvictionPolicyFactory`) are `public`.
"Use `Hotshelf.builder()` and the `Cache` interface" is a convention, not compiler-enforced. The roadmap's v3 Spring
integration grows inside this module (optional dependency + auto-configuration) instead of a separate starter.
