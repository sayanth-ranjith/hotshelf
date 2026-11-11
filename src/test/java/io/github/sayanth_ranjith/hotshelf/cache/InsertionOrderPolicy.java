package io.github.sayanth_ranjith.hotshelf.cache;

import io.github.sayanth_ranjith.hotshelf.eviction.EvictionPolicy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;

/**
 * Test-only fake policy: the victim is the oldest inserted key, and access does not change the order.
 * Records every call so tests can check how {@link LocalCache} drives its policy.
 */
final class InsertionOrderPolicy<K> implements EvictionPolicy<K> {

	private final SequencedSet<K> tracked = new LinkedHashSet<>();
	private final List<String> calls = new ArrayList<>();

	@Override
	public void recordInsert(K key) {
		Objects.requireNonNull(key, "key");
		calls.add("insert:" + key);
		tracked.add(key);
	}

	@Override
	public void recordAccess(K key) {
		Objects.requireNonNull(key, "key");
		calls.add("access:" + key);
	}

	@Override
	public void recordRemoval(K key) {
		Objects.requireNonNull(key, "key");
		calls.add("removal:" + key);
		tracked.remove(key);
	}

	@Override
	public void clear() {
		calls.add("clear");
		tracked.clear();
	}

	@Override
	public Optional<K> victim() {
		return tracked.isEmpty() ? Optional.empty() : Optional.of(tracked.getFirst());
	}

	/** Keys currently tracked, oldest first. */
	List<K> tracked() {
		return List.copyOf(tracked);
	}

	/** Every call received so far, in order, e.g. {@code "insert:a"}. */
	List<String> calls() {
		return List.copyOf(calls);
	}

}
