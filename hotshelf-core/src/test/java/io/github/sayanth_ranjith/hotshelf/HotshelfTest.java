package io.github.sayanth_ranjith.hotshelf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * A cache from {@link Hotshelf#newCache()} must honour the full contract, whatever class is behind it.
 */
class HotshelfTest extends CacheContractTest {

	@Override
	protected Cache<String, String> newCache() {
		return Hotshelf.newCache();
	}

	@Test
	void eachCallReturnsAnIndependentCache() {
		Cache<String, String> first = Hotshelf.newCache();
		Cache<String, String> second = Hotshelf.newCache();

		first.put("k", "v");

		assertThat(second).isNotSameAs(first);
		assertThat(second.get("k")).isEmpty();
	}

}
