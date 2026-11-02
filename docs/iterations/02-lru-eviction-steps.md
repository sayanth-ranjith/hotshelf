# Iteration 02 steps: bounded size and LRU eviction

This file breaks [02-lru-eviction.md](02-lru-eviction.md) into small steps. Each step is **one commit** on the
`iteration/02-lru-eviction` branch. Each one compiles, keeps the build green and teaches one idea. We stop after every step to review it.

The design, the patterns and the definition of done live in the main iteration doc. This file is only the order of work.

**Order:** bounded size comes first, with a fake policy in the tests. LRU is built afterwards and plugs in without changing
`LocalCache`. That shows DIP and OCP working *before* we rely on them.

| # | Step | New idea | Status |
|---|---|---|---|
| 1 | `EvictionPolicy` interface | Strategy: a contract for "who goes next" | ☑ |
| 2 | Bounded `LocalCache` | Composition + DIP: the cache only knows the interface; Null Object for "no eviction" | ☑ |
| 3 | LRU list: insert + victim | Doubly linked list with sentinel nodes | ☐ |
| 4 | LRU access: move to head | O(1) unlink, because we hold the node | ☐ |
| 5 | LRU removal and clear | Keeping map and list in sync | ☐ |
| 6 | Check LRU against `LinkedHashMap` | Differential testing against a trusted reference | ☐ |
| 7 | `Hotshelf.builder()` | Builder + validation; `newCache()` is removed | ☐ |
| 8 | Docs and PR | Learning log, README boxes, CLAUDE.md state | ☐ |

---

## Step 1: `EvictionPolicy` interface

**Write**
- `internal/eviction/EvictionPolicy<K>` (public, but the package is not exported; see ADR-007) with `recordInsert`, `recordAccess`, `recordRemoval`, `clear`, `victim()`.
- The javadoc of each method states its contract, including what happens for a key the policy has never seen.

**Why**
- **Strategy.** The cache asks *"who should go?"* without knowing *how* the answer is computed.
- **ISP.** Only keys go in. A policy never sees values.
- `clear()` is **added** compared with the sketch in the main doc. Without it, `cache.clear()` would leave every key
  tracked in the policy, which is a memory leak and leaves ghost victims behind.

**Test:** none yet, because an interface has no behaviour. The build must stay green.

**Commit:** `Iteration 02 step 1: EvictionPolicy interface`

---

## Step 2: bounded `LocalCache`

**Write**
- A `LocalCache(long maximumSize, EvictionPolicy<K> policy)` constructor. It rejects `maximumSize <= 0` and a null policy.
- `get` hit → `recordAccess`. A miss touches nothing.
- `put` existing key → update the value + `recordAccess`, **no eviction** (checking "already present?" *before*
  evicting is a pitfall from the main doc).
- `put` new key → if `size >= maximumSize`, evict `victim()` (remove from map and policy) **first**, then insert.
- `remove` of a present key → `recordRemoval`. `clear` → `policy.clear()`.
- `NoEvictionPolicy<K>`: a **Null Object**. It tracks nothing and never names a victim. The no-argument
  `LocalCache()` (still used by `Hotshelf.newCache()`) means unbounded + `NoEvictionPolicy`, so an unbounded cache
  doesn't pay for tracking keys it will never evict.

**Why**
- **Composition + DIP.** `LocalCache` *has an* `EvictionPolicy` and never names an implementation. LRU (and FIFO and LFU later)
  plugs in without touching this class (**OCP**).
- **Null Object** instead of `if (policy != null)` checks scattered through `LocalCache`.
- Tests use a test-only `InsertionOrderPolicy` (a fake). That proves the bounding logic before any real policy exists,
  and failures point at `LocalCache`, not at LRU.

**Tests**
- `LocalCacheTest` still passes the contract suite (unbounded), and a second subclass runs the contract on a bounded cache.
- New `BoundedLocalCacheTest`: `max + 1` evicts the policy's victim, `size() <= max` always holds, updating a key on a full cache
  doesn't evict, `remove` frees a slot, a miss doesn't record an access, evicted/removed/cleared keys are no longer tracked by
  the policy, and the constructor rejects bad arguments.

**Commit:** `Iteration 02 step 2: bounded LocalCache evicts through EvictionPolicy`

---

## Step 3: LRU list, insert and victim

**Write**
- `internal/eviction/LruEvictionPolicy<K>` with a private `Node<K>` (`key`, `prev`, `next`), a `HashMap<K, Node<K>>`
  and two **sentinel** nodes, `head` and `tail`, linked to each other when empty.
- `recordInsert(key)` links a new node right after `head` (most recent).
- `victim()` returns `tail.prev`'s key, or empty when `tail.prev == head`.
- `recordAccess`, `recordRemoval` and `clear` throw `UnsupportedOperationException` for now.

**Why sentinels?** With dummy head and tail nodes, every real node always has a non-null `prev` and `next`.
Insert and unlink become 4 lines with **no `if (node == head)` special cases**. That removes the most common source of linked-list bugs.

**Tests** (`LruEvictionPolicyTest`)
- An empty policy has no victim.
- Insert A, B, C → victim is A.
- Insert of a null key throws `NullPointerException`.

**Commit:** `Iteration 02 step 3: LRU list with sentinels, insert and victim`

---

## Step 4: LRU access, move to head

**Write**
- A private `unlink(node)` and `linkAfterHead(node)`.
- `recordAccess(key)` looks up the node, unlinks it and links it after head. An **unknown key is a no-op**: it
  creates no node (pitfall from the main doc).
- `recordInsert` of a key that is already tracked behaves like an access, so it never creates a duplicate node.

**Why O(1)?** The map gives us the node directly. A doubly linked node knows its `prev`, so unlinking is pointer
surgery, with no walk through the list.

**Tests**
- Insert A, B, C, access A → victim is B.
- Accessing an unknown key changes nothing and the victim stays the same.
- Inserting A twice → removing A once leaves nothing tracked (no duplicate node).

**Commit:** `Iteration 02 step 4: LRU access moves node to head`

---

## Step 5: LRU removal and clear

**Write**
- `recordRemoval(key)` removes the key from the map **and** unlinks its node. An unknown key is a no-op.
- `clear()` empties the map and relinks `head ↔ tail`.

**Why** Most LRU bugs come from a map and list that disagree. Every method now updates both, in the same place.

**Tests**
- Insert A, B, remove A → victim is B.
- Remove the only key → no victim (no ghost node).
- `clear()` → no victim, and the policy works normally afterwards.

**Commit:** `Iteration 02 step 5: LRU removal and clear`

---

## Step 6: check LRU against `LinkedHashMap`

**Write**
- A test that runs a few thousand random insert / access / remove operations (fixed seed, so it is reproducible) on
  both `LruEvictionPolicy` and a `LinkedHashMap` with `accessOrder = true`. After every operation, both must agree on the victim.

**Why** Hand-picked examples only cover the cases we thought of. A **differential test** compares our code with a
trusted implementation on inputs nobody picked by hand. The JDK's `LinkedHashMap` is the same idea (hash map + doubly linked list),
so it is the perfect reference.

**Commit:** `Iteration 02 step 6: differential test against LinkedHashMap`

---

## Step 7: `Hotshelf.builder()`

**Write**
- `HotshelfBuilder<K, V>` in the API package: `maximumSize(long)`, `evictionPolicy(EvictionPolicyType)`, `build()`.
- `EvictionPolicyType` enum (API package) with only `LRU` for now.
- The builder lives in the API package and must not name `internal` types in its signatures. A small public factory in
  `internal` turns `(maximumSize, EvictionPolicyType)` into a `LocalCache`. That factory is the *only* place that names `LruEvictionPolicy`.
- Remove `Hotshelf.newCache()`. `HotshelfTest` switches to the builder.

**Decisions to confirm at this step**
- Default when `maximumSize` is not set: unbounded (`NoEvictionPolicy`), or require it?
- `maximumSize <= 0` → `IllegalArgumentException`, thrown from `maximumSize(...)` (fail fast) or from `build()`?

**Tests**
- A cache from the builder passes the contract suite.
- The builder rejects `0` and negative sizes.
- End to end with LRU: `maximumSize(3)`, put A, B, C, get A, put D → **B** is evicted, not A.

**Commit:** `Iteration 02 step 7: Hotshelf.builder() replaces newCache()`

---

## Step 8: docs and PR

- Tick the definition of done in `02-lru-eviction.md` and fill in its learning log.
- README roadmap: tick `Cache<K, V>` (from iteration 01), LRU, max size. The builder box stays unticked until it has its later options.
- Update "Current state" in `CLAUDE.md`. Add an ADR only if a step made a significant decision.
- Push and open the PR. The description repeats the "why" (Strategy, DIP/OCP, ISP, Null Object, Builder, sentinels).

**Commit:** `Iteration 02: docs and learning log`
