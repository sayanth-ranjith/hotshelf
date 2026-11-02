package io.github.sayanth_ranjith.hotshelf.cache;

import io.github.sayanth_ranjith.hotshelf.CacheContractTest;

/**
 * A bounded cache must still honour the full contract while it has room.
 */
class BoundedLocalCacheContractTest extends CacheContractTest {

	@Override
	protected Cache<String, String> newCache() {
		return new LocalCache<>(100, new InsertionOrderPolicy<>());
	}

}
