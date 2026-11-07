package io.github.sayanth_ranjith.hotshelf.internal.eviction;

import io.github.sayanth_ranjith.hotshelf.internal.eviction.core.EvictionPolicy;

import java.util.Objects;

/**
 * Turns an {@link EvictionPolicyType} into a new policy instance. The only class that names the implementations.
 * <p>
 * Public only so {@code cache.HotshelfBuilder} can use it. Not part of the API.
 */
public final class EvictionPolicyFactory {

	private EvictionPolicyFactory() {
	}

	/** Returns a new, empty policy of the given type. Each cache needs its own instance. */
	public static <K> EvictionPolicy<K> create(EvictionPolicyType type) {
		Objects.requireNonNull(type, "type");
		// No default branch: a new enum constant without a case here is a compile error.
		return switch (type) {
			case LRU -> new LruEvictionPolicy<>();
		};
	}

}
