# CLAUDE.md

Guidance for Claude (and any other contributor) working on **hotshelf**.

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

- **Java 21 is required.** The machine currently has only JDK 17 on PATH (`C:\Program Files\Java\jdk-17.0.1`).
  Installing JDK 21 is part of iteration 00.
- Build and run the tests: `.\mvnw.cmd verify` (PowerShell) or `./mvnw verify` (bash).
- Run a single test class: `.\mvnw.cmd test -Dtest=LruEvictionPolicyTest`.

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
