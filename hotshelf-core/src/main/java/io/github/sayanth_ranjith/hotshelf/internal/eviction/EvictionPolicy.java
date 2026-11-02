package io.github.sayanth_ranjith.hotshelf.internal.eviction;

import java.util.Optional;

/**
 * Decides which key a bounded cache drops when it is full.
 * <p>
 * The cache reports what happens to its keys, and the policy answers {@link #victim()}. The policy only sees keys,
 * never values. It does not remove anything from the cache itself: the cache asks for a victim, removes that entry,
 * then calls {@link #recordRemoval}.
 * <p>
 * Implementations are not thread-safe. The owning cache guards them.
 * <p>
 * Public only so {@code cache.LocalCache} can use it.
 * Not part of the API: the module does not export this package.
 *
 * @param <K> key type
 */
public interface EvictionPolicy<K> {

	/**
	 * A new key was added to the cache. If the key is already tracked, this behaves like {@link #recordAccess}.
	 *
	 * @throws NullPointerException if {@code key} is null
	 */
	void recordInsert(K key);

	/**
	 * A tracked key was read or overwritten. A key that is not tracked is ignored and does not start being tracked.
	 *
	 * @throws NullPointerException if {@code key} is null
	 */
	void recordAccess(K key);

	/**
	 * A key left the cache. A key that is not tracked is ignored.
	 *
	 * @throws NullPointerException if {@code key} is null
	 */
	void recordRemoval(K key);

	/** The cache was cleared. Stop tracking every key. */
	void clear();

	/**
	 * The key that should be evicted next, or empty if no key is tracked. Does not change the policy's state.
	 */
	Optional<K> victim();

}
