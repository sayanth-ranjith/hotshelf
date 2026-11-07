package io.github.sayanth_ranjith.hotshelf.cache;

import io.github.sayanth_ranjith.hotshelf.internal.eviction.EvictionPolicyType;
import io.github.sayanth_ranjith.hotshelf.internal.eviction.EvictionPolicyFactory;

import java.util.Objects;

/**
 * Configures and creates a {@link Cache}. Get one from {@link Hotshelf#builder()}.
 * <p>
 * With no options set, {@link #build()} returns an unbounded cache. Setting {@link #maximumSize} bounds it, and the
 * eviction policy defaults to {@link EvictionPolicyType#LRU}.
 * <p>
 * A builder can be reused: every {@link #build()} returns a new, independent cache. Not thread-safe.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class HotshelfBuilder<K, V> {

	private static final long NOT_SET = 0;

	private long maximumSize = NOT_SET;
	private EvictionPolicyType evictionPolicy;

	HotshelfBuilder() {
	}

	/**
	 * Bounds the cache to at most {@code maximumSize} entries. Adding a new key to a full cache evicts one entry first.
	 *
	 * @throws IllegalArgumentException if {@code maximumSize} is zero or negative
	 */
	public HotshelfBuilder<K, V> maximumSize(long maximumSize) {
		if (maximumSize <= 0) {
			throw new IllegalArgumentException("maximumSize must be positive: " + maximumSize);
		}
		this.maximumSize = maximumSize;
		return this;
	}

	/**
	 * Chooses which entry is evicted when the cache is full. Requires {@link #maximumSize}.
	 *
	 * @throws NullPointerException if {@code evictionPolicy} is null
	 */
	public HotshelfBuilder<K, V> evictionPolicy(EvictionPolicyType evictionPolicy) {
		this.evictionPolicy = Objects.requireNonNull(evictionPolicy, "evictionPolicy");
		return this;
	}

	/**
	 * Creates a new cache with the options set so far.
	 *
	 * @throws IllegalStateException if an eviction policy was set without a maximum size
	 */
	public Cache<K, V> build() {
		if (maximumSize == NOT_SET) {
			if (evictionPolicy != null) {
				throw new IllegalStateException("evictionPolicy requires maximumSize: an unbounded cache never evicts");
			}
			return new LocalCache<>();
		}
		EvictionPolicyType type = evictionPolicy != null ? evictionPolicy : EvictionPolicyType.LRU;
		return new LocalCache<>(maximumSize, EvictionPolicyFactory.create(type));
	}

}
