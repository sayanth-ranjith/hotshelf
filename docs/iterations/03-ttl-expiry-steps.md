# Iteration 03 steps: TTL expiry

This file breaks [03-ttl-expiry.md](03-ttl-expiry.md) into small steps. Each step is **one commit** on the
`iteration/03-ttl-expiry` branch. Each one compiles, keeps the build green and teaches one idea. We stop after every step to review it.

The design, the patterns and the definition of done live in the main iteration doc. This file is the order of work
plus the **open questions** that must be answered before or during the steps.

**Order:** time first (`Ticker`), then a pure refactor that stores entries with a timestamp but never expires them,
then the expiry rule, then the builder. Expiry arrives only when the tests can already control time.

| # | Step | New idea | Status |
|---|---|---|---|
| 1 | `Ticker` + `FakeTicker` | DIP: depend on "a source of time", not on the system clock | ☐ |
| 2 | `CacheEntry` record in `LocalCache` | Refactor under a green build: change the shape, not the behaviour | ☐ |
| 3 | Lazy expiry in `get` | Expired = miss; overflow-safe `nanoTime` comparison | ☐ |
| 4 | `expireAfterWrite(Duration)` in the builder | Builder validation; `Duration` to nanos | ☐ |
| 5 | Docs and PR | Learning log, README boxes, CLAUDE.md state | ☐ |

---

## Open questions (answer these)

Write your answer under each one. My recommendation is marked, but it's your call.

### Q1. What does `size()` return while expired entries are still stored?
Lazy expiry means an expired entry stays in the map until someone reads it.
- **(a) Count them.** O(1). The number can be higher than the number of live entries. Caffeine does this and names
  its method `estimatedSize()` to say so.
- **(b) Scan and purge first.** Exact, but `size()` becomes O(n), and a read-looking method mutates the cache.

**Recommendation: (a)**, documented in the `Cache.size()` Javadoc. Exactness belongs to the active sweeper in iteration 10.

**Your answer:**

### Q2. Should `ticker(Ticker)` on the builder be public?
Tests need to inject a `FakeTicker` through the builder.
- **(a) Package-private.** Only tests in the root package can use it. The public API stays small.
- **(b) Public**, like Caffeine's `Caffeine.ticker(...)`. Users can test *their own* code that uses a hotshelf cache
  without sleeping.

**Recommendation: (b).** The argument for "testable time" applies to our users too, not only to us. The cost is one more
public method we must keep stable.

**Your answer:**

### Q3. Where does the "is it expired?" rule live?
- **(a) In `LocalCache`.** Two fields (`ttlNanos`, `ticker`) and one private `isExpired(entry, now)` (SRP within the
  class). This is what the main doc plans.
- **(b) A separate `Expiry` Strategy** with a Null Object `NeverExpire`, mirroring `EvictionPolicy` / `NoEvictionPolicy`.

**Recommendation: (a) now.** There is only one expiry rule, so a Strategy would be an interface with one real
implementation (YAGNI). When `expireAfterAccess` arrives, that's the second rule and the right moment to extract (b).
Add it to the "Later" list.

**Your answer:**

### Q4. What happens with a huge TTL, like `Duration.ofDays(365 * 1000)`?
`Duration.toNanos()` throws `ArithmeticException` above about 292 years, because the nanos don't fit in a `long`.
- **(a) Reject it** with `IllegalArgumentException` in the builder.
- **(b) Clamp it** to `Long.MAX_VALUE` nanos, which in practice means "never expires". Caffeine does this.

**Recommendation: (b).** Someone writing "1000 years" means "basically forever", not "crash".

**Your answer:**

### Q5. Is `expireAfterWrite` allowed without `maximumSize`?
Today an unbounded cache uses `new LocalCache<>()` with `NoEvictionPolicy`.
- **(a) Yes.** An unbounded cache with TTL is a common, valid setup ("keep everything, but at most 10 minutes").
- **(b) No**, require `maximumSize`, like `evictionPolicy` does today.

**Recommendation: (a).** TTL and size are independent limits. Note the trade-off: with lazy expiry, an unbounded cache
with TTL can still grow forever if expired keys are never read again. The sweeper in iteration 10 fixes that.

**Your answer:**

### Q6. Where do the loose ends of iteration 02 go?
Still open: README roadmap boxes for 01 and 02, the definition-of-done boxes in `02-lru-eviction.md`, its learning log,
and step 8 of `02-lru-eviction-steps.md`.
- **(a) A separate `docs/02-wrap-up` branch and small PR**, before 03 starts.
- **(b) The first commit on this branch.**

**Recommendation: (a).** One iteration per PR keeps the 03 diff about TTL only. The learning log should be written
by you anyway, since it records what *you* learned.

**Your answer:**

---

## Step 1: `Ticker` and `FakeTicker`

**Write**
- `expiry/Ticker` (new package): `long read()` returning nanoseconds from an arbitrary origin, and a static
  `Ticker.system()` that returns `System::nanoTime`.
- `src/test/.../expiry/FakeTicker`: starts at 0, `advance(Duration)`, `read()` returns the current value.

**Why**
- **DIP.** `LocalCache` will ask "what time is it?" through an interface. The real clock is a detail plugged in at
  build time. Tests plug in a fake one and move time by hand, so a "10 minute TTL" test runs in microseconds.
- **`nanoTime`, not `currentTimeMillis`** (ADR-004). Wall-clock time can jump backwards (NTP, someone changing the
  clock). `nanoTime` only moves forward, but its absolute value means nothing, so only *differences* are compared.

**Test:** `FakeTickerTest`: starts at 0, `advance` adds up. A sanity check that `Ticker.system()` doesn't go backwards
between two reads.

**Commit:** `Iteration 03 step 1: Ticker abstraction and FakeTicker`

---

## Step 2: `CacheEntry` record

**Write**
- `cache/CacheEntry<V>(V value, long writeTimeNanos)`, a package-private record.
- `LocalCache` stores `Map<K, CacheEntry<V>>`. `put` writes `new CacheEntry<>(value, ticker.read())`. `get` unwraps.
- `LocalCache` takes a `Ticker` in its constructor (defaulting to `Ticker.system()` in the existing constructors).
- **No expiry yet.** Behaviour is identical to before.

**Why**
- **Immutable value object.** Overwriting a key creates a new entry with a new write time. Nothing is ever modified
  in place, so "reset the TTL on overwrite" is automatic, not something to remember.
- **Refactor under a green build.** Changing the data shape and changing behaviour in separate commits means that if a
  test breaks here, it's the refactor's fault, not the expiry logic's.

**Test:** the whole existing suite (contract tests, bounded tests, LRU tests) stays green with no changes.

**Commit:** `Iteration 03 step 2: store entries with their write time`

---

## Step 3: lazy expiry in `get`

**Write**
- A `ttlNanos` field (`Long.MAX_VALUE` or a "no TTL" marker when unset) and a new constructor that takes it.
- `private boolean isExpired(CacheEntry<V> entry, long now)`: `now - entry.writeTimeNanos() >= ttlNanos`.
- `get`: look up the entry. If it's expired, remove it from the map **and** call `policy.recordRemoval`, then return
  `Optional.empty()`. Only a live hit calls `recordAccess`.

**Why**
- **Overflow-safe comparison.** `now - writeTime >= ttl` is correct even if `nanoTime` wraps around.
  `now >= writeTime + ttl` is not: the addition can overflow and turn negative.
- **Map and policy agree.** The same rule as in 02: every path that removes from the map also removes from the policy,
  or LRU keeps a ghost key and later names a victim that isn't there.
- **No `recordAccess` on an expired hit.** Otherwise LRU would mark as "recently used" a key that's about to be deleted.

**Test:** `ExpiringLocalCacheTest` with `FakeTicker`:
- returned at `ttl - 1ns`, absent at exactly `ttl`
- overwriting resets the TTL
- after an expired `get`, the policy no longer tracks the key (use a recording or insertion-order test policy)
- with no TTL, an entry is still there after advancing a very long time

**Commit:** `Iteration 03 step 3: lazy expireAfterWrite on get`

---

## Step 4: builder

**Write**
- `expireAfterWrite(Duration)`: rejects null (NPE), and zero or negative (IAE). Huge values per Q4.
- `ticker(Ticker)`: visibility per Q2. Rejects null.
- `build()` passes the TTL and ticker to `LocalCache`, for bounded and unbounded caches (per Q5).

**Why**
- **Builder.** Two new optional settings, and the constructor already has several parameters. The builder keeps call
  sites readable and puts validation in one place, so `LocalCache` never sees an invalid TTL.

**Test:** `HotshelfBuilderTest` additions: validation, and one end-to-end test that builds a cache with a `FakeTicker`
and sees an entry expire.

**Commit:** `Iteration 03 step 4: expireAfterWrite in the builder`

---

## Step 5: docs and PR

- Tick the definition-of-done boxes in [03-ttl-expiry.md](03-ttl-expiry.md) and the TTL box in the README roadmap.
- Record the `size()` decision (Q1) in the `Cache.size()` Javadoc and the learning log.
- Add ADRs for any answer above that's a real decision (Q1, Q4 at least).
- Update the "Current state" section of `CLAUDE.md`.
- Learning log: written by you.

---

## Things that are out of scope (and where they go)
- `expireAfterAccess` (sliding expiry): a stretch goal, and the trigger for the `Expiry` Strategy (Q3).
- Preferring an expired entry over the LRU victim when the cache is full: lazy expiry doesn't know which entries are
  expired without scanning, so a full cache may evict a live entry while dead ones sit around. Iteration 10.
- Background cleanup: iteration 10.
