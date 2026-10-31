package io.github.sayanth_ranjith.hotshelf.internal;

import io.github.sayanth_ranjith.hotshelf.Cache;
import io.github.sayanth_ranjith.hotshelf.CacheContractTest;

class LocalCacheTest extends CacheContractTest {

	@Override
	protected Cache<String, String> newCache() {
		return new LocalCache<>();
	}

}
