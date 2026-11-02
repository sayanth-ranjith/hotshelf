package io.github.sayanth_ranjith.hotshelf.cache;

import io.github.sayanth_ranjith.hotshelf.internal.eviction.EvictionPolicy;
import io.github.sayanth_ranjith.hotshelf.internal.eviction.NoEvictionPolicy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Single-threaded {@link Cache} backed by a {@link HashMap}, optionally bounded. Not thread-safe.
 * <p>
 * When a new key would push the size past {@code maximumSize}, the entry named by the {@link EvictionPolicy} is
 * evicted first. The map and the policy always track the same keys.
 * <p>
 * Package-private: callers get caches from {@link Hotshelf} and only ever see the {@link Cache} interface.
 */
final class LocalCache<K, V> implements Cache<K, V> {

	private final Map<K, V> entries = new HashMap<>();
	private final long maximumSize;
	private final EvictionPolicy<K> policy;

	/** An unbounded cache. Nothing is ever evicted, so no keys are tracked. */
	LocalCache() {
		this(Long.MAX_VALUE, new NoEvictionPolicy<>());
	}

	LocalCache(long maximumSize, EvictionPolicy<K> policy) {
		if (maximumSize <= 0) {
			throw new IllegalArgumentException("maximumSize must be positive: " + maximumSize);
		}
		this.maximumSize = maximumSize;
		this.policy = Objects.requireNonNull(policy, "policy");
	}

	@Override
	public Optional<V> get(K key) {
		Objects.requireNonNull(key, "key");
		V value = entries.get(key);
		if (value != null) {
			policy.recordAccess(key);
		}
		return Optional.ofNullable(value);
	}

	@Override
	public void put(K key, V value) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(value, "value");
		if (entries.containsKey(key)) {
			entries.put(key, value);
			policy.recordAccess(key);
			return;
		}
		if (entries.size() >= maximumSize) {
			evictOne();
		}
		entries.put(key, value);
		policy.recordInsert(key);
	}

	@Override
	public void remove(K key) {
		Objects.requireNonNull(key, "key");
		if (entries.remove(key) != null) {
			policy.recordRemoval(key);
		}
	}

	@Override
	public void clear() {
		entries.clear();
		policy.clear();
	}

	@Override
	public int size() {
		return entries.size();
	}

	private void evictOne() {
		K victim = policy.victim()
				.orElseThrow(() -> new IllegalStateException("cache is full but the policy named no victim"));
		entries.remove(victim);
		policy.recordRemoval(victim);
	}

}
