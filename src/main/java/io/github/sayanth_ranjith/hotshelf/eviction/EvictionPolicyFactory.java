package io.github.sayanth_ranjith.hotshelf.eviction;


import java.util.Objects;

/**
 * Turns an {@link EvictionPolicyType} into a new policy instance. The only class that names the implementations.
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
