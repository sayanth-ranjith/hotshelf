# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

It also applies to any other contributor working on **hotshelf**.

## What this project is

hotshelf is a **learn-by-building** in-memory caching library for Java 21, with Spring Boot integration planned later.
The author (Sayanth) is a backend developer who knows caching at the "check the cache, else hit the DB" level
and knows Redis keeps data in RAM. **The point of the project is understanding how caches work inside**, not just
shipping working code. So:

- **Teach as you build.** Every solution states which design pattern(s) and SOLID principle(s) it uses, **why** they
  fit here, and which alternative was rejected and why. That explanation goes in the iteration doc and the PR
  description. Keep code comments short and about the code itself.
- **Introduce one new concept at a time.** Follow the iteration plan in [`docs/iterations/`](docs/iterations/).
  One iteration is one branch and one PR. Don't jump ahead. Good ideas that belong to a later iteration go in that
  iteration's "Later" list.
- **Prefer composition over inheritance.** Prefer small interfaces and immutable value objects. See
  [`docs/design-principles.md`](docs/design-principles.md).
- Record significant decisions as short entries in [`docs/decisions.md`](docs/decisions.md).

## Docs map

| File | Purpose |
|---|---|
| `README.md` | Public pitch, planned API, roadmap checklist (tick boxes as iterations land) |
| `docs/README.md` | How the docs are organised |
| `docs/roadmap.md` | All iterations at a glance, v1 to v4 |
| `docs/design-principles.md` | SOLID and design patterns as applied in hotshelf |
| `docs/decisions.md` | Architecture decision log |
| `docs/iterations/NN-*.md` | Detailed plan, design rationale, definition of done and learning log per iteration |

## Build and test

- **Java 21 is required** (`<java.version>21</java.version>` in the parent `pom.xml`). JDK 17 cannot build it.
  - JDK 21 lives at `%USERPROFILE%\.jdks\ms-21.0.7`. The user-level `JAVA_HOME` points there, and `mvnw` uses `JAVA_HOME`.
  - The **system** PATH still starts with Oracle's `javapath` (JDK 17), so a bare `java -version` may print 17. That doesn't affect Maven.
  - IntelliJ's project SDK (`ms-21`) applies only to IntelliJ's own runners. Its Terminal tab inherits Windows env vars
    from when IntelliJ was started.
  - If a shell still reports 17, set it for that shell: `$env:JAVA_HOME="$env:USERPROFILE\.jdks\ms-21.0.7"`.
- Build and run the tests: `.\mvnw.cmd verify` (PowerShell) or `./mvnw verify` (bash).
- Run a single test class: `.\mvnw.cmd test -Dtest=BuildSanityTest`. Run a single method: `-Dtest=BuildSanityTest#junitAndAssertjAreWired`.
- There is no linter or formatter configured. Source files use tabs for indentation (Initializr default).
- After iteration 00 the build is multi-module. To build or test only the core module, add `-pl hotshelf-core`.

## Current state vs target architecture

**Today** (after iteration 00) the build is a plain parent POM (`hotshelf-parent`, not Spring Boot's parent) with one
module, `hotshelf-core`: plain Java 21, zero runtime dependencies (ADR-001), JUnit 5 and AssertJ for tests only.
Plugin and test-library versions are pinned in the parent. No cache code exists yet; `BuildSanityTest` is the only test.
Check `git log` and the README roadmap checkboxes to see which iterations have landed.

**Target v1 design.** Each piece arrives in the iteration shown and is described in `docs/iterations/`:
- `Cache<K, V>` is the public interface (01). `LocalCache` is the package-private, single-threaded implementation.
  A shared abstract **contract test** suite runs against every implementation.
- `Hotshelf.builder()` is the fluent **Builder** and the only way to get a cache (02). It replaces the temporary `Hotshelf.newCache()`.
- `EvictionPolicy` is a **Strategy** (02). LRU is O(1): a `HashMap` plus a hand-written doubly linked list.
  FIFO and LFU are added in v2 without changing `LocalCache`, which is the OCP test.
- TTL is lazy, checked on read, and uses an injected `Ticker` that reads `System.nanoTime` (03, ADR-004).
- Thread safety is a `ConcurrentCache` **Decorator** around the single-threaded cache, holding one `ReentrantLock` (04).
  A read/write lock is deliberately not used, because an LRU `get` mutates the list (ADR-003).
  Segmented locking comes in v2 (iteration 11).
- Stats use a **Null Object** recorder when stats are disabled, and `CacheStats` is an immutable snapshot (05).
- `get(key, loader)` does single-flight loading to prevent stampedes (06).
- v3 adds `hotshelf-spring-boot-starter`, which adapts hotshelf to Spring's `CacheManager`. v4 adds an L1 + Redis L2 layer.

## Code conventions

- Root package: `io.github.sayanth_ranjith.hotshelf`.
- The **core library has zero runtime dependencies**: no Spring, no Guava. Spring lives only in the future starter module.
- Keep the public API small: interfaces, the `Hotshelf` entry point, value types. Implementation classes are package-private.
- `null` keys and values are rejected with `NullPointerException`. "Absent" is expressed with `Optional`.
- Time is never read directly. Use the `Ticker` abstraction so tests can control time. **No `Thread.sleep` in tests.**
- Tests: JUnit 5 and AssertJ. Every behaviour in an iteration's definition of done has a test.
- Concurrency code must say in a comment which lock guards which state.

## Git workflow

- **Never commit to `main`.** Create a branch (`iteration/NN-short-name`, or `docs/...` and `chore/...` for non-feature work),
  commit, push, and give the author the PR link. The author reviews and merges.
- Commits are authored as `ranjithsayanth@gmail.com` (set in the repo-local git config). Add a Claude co-author line when Claude contributed.
- The `gh` CLI is not installed. The PR link format is `https://github.com/sayanth-ranjith/hotshelf/pull/new/<branch>`.
- If `git push` fails with "terminal prompts disabled", rerun it with the credential manager allowed to prompt:
  `$env:GIT_TERMINAL_PROMPT='1'; $env:GCM_INTERACTIVE='always'; git push -u origin <branch>`.
