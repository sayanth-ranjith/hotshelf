package io.github.sayanth_ranjith.hotshelf;

/**
 * Entry point for creating caches.
 */
public final class Hotshelf {

	private Hotshelf() {
	}

	/**
	 * Creates an empty, unbounded, single-threaded cache.
	 * <p>
	 * Temporary: replaced by {@code Hotshelf.builder()} in iteration 02.
	 */
	public static <K, V> Cache<K, V> newCache() {
		return new LocalCache<>();
	}

}
