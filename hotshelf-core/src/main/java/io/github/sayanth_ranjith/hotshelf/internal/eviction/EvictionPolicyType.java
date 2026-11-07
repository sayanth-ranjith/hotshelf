package io.github.sayanth_ranjith.hotshelf.internal.eviction;

import io.github.sayanth_ranjith.hotshelf.cache.HotshelfBuilder;

/**
 * Which entry a bounded cache drops when it is full. Chosen on .
 */
public enum EvictionPolicyType {

	/** Least recently used: the entry that was read or written longest ago goes first. */
	LRU

}
