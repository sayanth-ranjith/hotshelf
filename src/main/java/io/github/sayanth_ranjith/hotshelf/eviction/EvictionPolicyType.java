package io.github.sayanth_ranjith.hotshelf.eviction;

import io.github.sayanth_ranjith.hotshelf.HotshelfBuilder;

/**
 * Which entry a bounded cache drops when it is full. Chosen with {@link HotshelfBuilder#evictionPolicy}.
 */
public enum EvictionPolicyType {

	/** Least recently used: the entry that was read or written longest ago goes first. */
	LRU

}
