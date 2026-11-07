package io.github.sayanth_ranjith.hotshelf.cache;

/**
 * Entry point for creating caches.
 *
 * <pre>{@code
 * Cache<String, User> users = Hotshelf.<String, User>builder()
 *         .maximumSize(1_000)
 *         .build();
 * }</pre>
 */
public final class Hotshelf {

	private Hotshelf() {
	}

	/** Returns a new builder with no options set. */
	public static <K, V> HotshelfBuilder<K, V> builder() {
		return new HotshelfBuilder<>();
	}

}
