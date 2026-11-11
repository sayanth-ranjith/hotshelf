package io.github.sayanth_ranjith.hotshelf.eviction;


import java.util.Objects;
import java.util.Optional;

/**
 * Null Object policy for unbounded caches: tracks nothing and never names a victim.
 */
public final class NoEvictionPolicy<K> implements EvictionPolicy<K> {

	@Override
	public void recordInsert(K key) {
		Objects.requireNonNull(key, "key");
	}

	@Override
	public void recordAccess(K key) {
		Objects.requireNonNull(key, "key");
	}

	@Override
	public void recordRemoval(K key) {
		Objects.requireNonNull(key, "key");
	}

	@Override
	public void clear() {
	}

	@Override
	public Optional<K> victim() {
		return Optional.empty();
	}

}
