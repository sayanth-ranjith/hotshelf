package io.github.sayanth_ranjith.hotshelf.internal;

import io.github.sayanth_ranjith.hotshelf.Cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Unbounded, single-threaded {@link Cache} backed by a {@link HashMap}. Not thread-safe.
 * <p>
 * Public only so {@link io.github.sayanth_ranjith.hotshelf.Hotshelf} can create it. Not part of the API.
 */
public final class LocalCache<K, V> implements Cache<K, V> {

	private final Map<K, V> entries = new HashMap<>();

	@Override
	public Optional<V> get(K key) {
		Objects.requireNonNull(key, "key");
		return Optional.ofNullable(entries.get(key));
	}

	@Override
	public void put(K key, V value) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(value, "value");
		entries.put(key, value);
	}

	@Override
	public void remove(K key) {
		Objects.requireNonNull(key, "key");
		entries.remove(key);
	}

	@Override
	public void clear() {
		entries.clear();
	}

	@Override
	public int size() {
		return entries.size();
	}

}
