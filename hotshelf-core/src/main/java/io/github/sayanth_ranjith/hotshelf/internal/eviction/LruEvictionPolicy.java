package io.github.sayanth_ranjith.hotshelf.internal.eviction;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;

/**
 * Least-recently-used policy backed by a {@link LinkedHashMap} in access order.
 * <p>
 * With {@code accessOrder = true}, every {@code get} or {@code put} of a present key moves it to the end, so the
 * first key in iteration order is always the least recently used one. All operations are O(1).
 * Only the keys matter; the map's values are a placeholder.
 * <p>
 * Not thread-safe; the owning cache guards it.
 */
final class LruEvictionPolicy<K> implements EvictionPolicy<K> {

	private final LinkedHashMap<K, Boolean> order = new LinkedHashMap<>(16, 0.75f, true);

	@Override
	public void recordInsert(K key) {
		Objects.requireNonNull(key, "key");
		order.put(key, Boolean.TRUE);
	}

	@Override
	public void recordAccess(K key) {
		Objects.requireNonNull(key, "key");
		order.get(key);
	}

	@Override
	public void recordRemoval(K key) {
		Objects.requireNonNull(key, "key");
		order.remove(key);
	}

	@Override
	public void clear() {
		order.clear();
	}

	@Override
	public Optional<K> victim() {
		return order.isEmpty() ? Optional.empty() : Optional.of(order.firstEntry().getKey());
	}

}
