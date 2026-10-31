package io.github.sayanth_ranjith.hotshelf;

import java.util.Optional;

/**
 * A key-value cache: a fast, possibly incomplete copy of data that lives somewhere slower.
 * <p>
 * A cache may forget an entry at any time (for example, when it is full or the entry is old), so callers must
 * always be ready to load the value from its source on a miss.
 * <p>
 * Neither keys nor values may be {@code null}. Every method that takes a key or value throws
 * {@link NullPointerException} when given {@code null}. Absence is expressed with {@link Optional}.
 *
 * @param <K> the key type; must have consistent {@code equals} and {@code hashCode}
 * @param <V> the value type
 */
public interface Cache<K, V> {

	/**
	 * Returns the value stored for {@code key}.
	 *
	 * @return the value on a hit, or {@link Optional#empty()} on a miss
	 * @throws NullPointerException if {@code key} is {@code null}
	 */
	Optional<V> get(K key);

	/**
	 * Stores {@code value} for {@code key}, replacing any value already stored for that key.
	 *
	 * @throws NullPointerException if {@code key} or {@code value} is {@code null}
	 */
	void put(K key, V value);

	/**
	 * Removes the entry for {@code key}. Does nothing if there is no such entry.
	 *
	 * @throws NullPointerException if {@code key} is {@code null}
	 */
	void remove(K key);

	/**
	 * Removes all entries.
	 */
	void clear();

	/**
	 * Returns the number of entries currently stored.
	 */
	int size();

}
