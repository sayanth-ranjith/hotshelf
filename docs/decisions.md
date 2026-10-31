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

---

## ADR-005: API package plus an unexported `internal` package
**Status:** Accepted (iteration 01)

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
