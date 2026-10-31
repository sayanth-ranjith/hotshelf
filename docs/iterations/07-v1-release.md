# Iteration 07: v1 release

## Goal
Measure, polish and tag **v0.1.0**. The README's planned API works exactly as written.

## Concepts you'll learn
- **Benchmarking on the JVM is hard.** JIT warm-up, dead-code elimination and GC noise all distort naive timing loops. **JMH** exists to handle that.
- Reading benchmark results: throughput vs latency, scaling with thread count.
- **API review**: what must be public, and what can be hidden. Every public type is something we have to keep stable.
- Semantic versioning: why `0.x` means "the API may still change".

## Scope
**In**
- `hotshelf-benchmarks` module (JMH) comparing hotshelf vs `ConcurrentHashMap` (no eviction, the upper bound) vs Caffeine,
  with 1, 4 and 8 threads, for read-heavy and mixed workloads.
- Public API audit: only `Cache`, `CacheStats`, `Hotshelf`, `HotshelfBuilder`, `EvictionPolicyType` (and `Ticker` if exposed)
  are public. Everything else is package-private.
- Javadoc on every public type and method.
- README: tick the v1 boxes, add a "Getting started" section and a benchmark results table.
- Fill in the empty learning logs and write a short v1 retrospective.
- Tag `v0.1.0`.

**Out**
- Publishing to Maven Central. Optional, and would be its own chore.

## Design
| Decision | Pattern / principle | Why |
|---|---|---|
| `Hotshelf` is the single entry point | **Facade** | One class to learn. The whole graph of `LocalCache`, the policy, the ticker, the stats and the decorator is assembled behind it. |
| Benchmarks in a separate module | Separation of concerns | JMH dependencies never leak into the core artifact |
| Minimal public surface | **Information hiding** | v2 can rewrite internals, such as segmented locking, without breaking users |

## Definition of done
- [ ] The README v1 code sample compiles and runs as a test.
- [ ] Benchmarks run with `.\mvnw.cmd -pl hotshelf-benchmarks package` and their results are recorded in the README.
- [ ] Every v1 iteration doc has its learning log filled in.
- [ ] `v0.1.0` tag pushed.

## Expectation setting
hotshelf **will** be slower than Caffeine, possibly by a lot under many threads, because of the single lock. That's the point of
the numbers: they motivate iteration 11 (segmented locking) and show *what* Caffeine's clever design buys.

## Learning log
*(fill in after the iteration)*
