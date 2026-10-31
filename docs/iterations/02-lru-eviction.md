# Iteration 02: Bounded size and LRU eviction

## Goal
The cache gets a `maximumSize`. When a `put` would exceed it, the **least recently used** entry is evicted, and every
operation stays **O(1)**. We also introduce the builder that will grow with every later iteration.

```java
Cache<String, User> users = Hotshelf.<String, User>builder()
        .maximumSize(1_000)
        .build();
```

## Concepts you'll learn
- **Why caches must forget.** Memory is finite. An unbounded cache is a memory leak with a nice name.
- **Eviction policy**: *which* entry to drop. LRU bets that "used recently" predicts "will be used again soon" (temporal locality).
- The classic **O(1) LRU**: a `HashMap` for lookup plus a **doubly linked list** for recency order.
  - Access a key → unlink its node and move it to the head: O(1), because we hold a direct reference to the node.
  - Evict → remove the tail: O(1).
  - Why a *doubly* linked list? To unlink a node you need its predecessor. A singly linked list would need an O(n) walk to find it.
- Aside: `java.util.LinkedHashMap(accessOrder = true)` does exactly this. We build it by hand to understand it, and we may
  compare against it in a test.

## Scope
**In**
- `EvictionPolicy<K>` interface and `LruEvictionPolicy<K>` implementation with a hand-written doubly linked list, using sentinel head and tail nodes.
- `LocalCache` takes an `EvictionPolicy` and a `maximumSize` in its constructor.
- `HotshelfBuilder<K, V>` with `maximumSize(long)` and `build()`, created via `Hotshelf.builder()`. `Hotshelf.newCache()` is removed.
- `EvictionPolicyType` enum on the builder (`LRU` only for now), matching the README's `.evictionPolicy(...)`.

**Out**
- LFU and FIFO (iteration 08). They should need *zero* changes to `LocalCache`, and that's the test of this design.

## Design

```java
interface EvictionPolicy<K> {          // package-private
    void recordInsert(K key);
    void recordAccess(K key);
    void recordRemoval(K key);
    Optional<K> victim();              // who should go next? (empty if nothing tracked)
}
```

```
LocalCache ── store: HashMap<K, V>
     │
     └── policy: EvictionPolicy<K>  ◄── LruEvictionPolicy (HashMap<K, Node> + doubly linked list)
```

| Decision | Pattern / principle | Why |
|---|---|---|
| Eviction logic behind `EvictionPolicy` | **Strategy** | Policies vary independently of storage. The builder picks one at runtime. |
| `LocalCache` never names `LruEvictionPolicy` | **DIP**, **OCP** | New policies plug in without editing the cache |
| Policy sees only keys | **ISP** | A policy has no business reading values |
| Fluent builder with `build()` validation | **Builder** | More options arrive in every iteration. A builder avoids telescoping constructors (`new LocalCache(size, ttl, ticker, stats, ...)`) and gives a single place for validation, e.g. `maximumSize <= 0` → `IllegalArgumentException`. |
| `Hotshelf.builder()` static method | **Static factory** | Discoverable entry point; keeps implementation classes package-private |

**Trade-off we accept:** the LRU policy keeps its own `HashMap<K, Node>`, so a lookup touches two maps.
Production caches like Caffeine store the list pointers *inside* the cache entry to avoid this. Keeping them separate is
clearer and keeps the Strategy clean. If the iteration 07 benchmarks show it matters, we can optimise it with numbers to back that up.

**Rejected:** `class LruCache extends LocalCache`. See "Why Strategy" in [design-principles.md](../design-principles.md).

## Definition of done
- [ ] Inserting `max + 1` entries evicts the oldest untouched one.
- [ ] `get` refreshes recency: put A, B, C (max 3), get A, put D → **B** is evicted, not A.
- [ ] `put` on an existing key updates the value, refreshes recency and does **not** evict.
- [ ] `remove` also removes the key from the policy, so no ghost nodes remain.
- [ ] `size()` ≤ `maximumSize` after every operation.
- [ ] Unit tests for `LruEvictionPolicy` on its own, plus `LocalCache` still passing the contract suite.
- [ ] Builder rejects non-positive sizes.

## Pitfalls
- Forgetting to update *both* the map and the list on every path. Most LRU bugs are a map and list that disagree.
- Evicting *before* checking whether the key already exists, which evicts an entry for no reason on an update.
- `get` of a missing key calling `recordAccess` and creating a node.

## Learning log
*(fill in after the iteration)*
