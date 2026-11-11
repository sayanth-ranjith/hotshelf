package io.github.sayanth_ranjith.hotshelf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * A cache from {@link Hotshelf#builder()} must honour the full contract, whatever class is behind it.
 */
class HotshelfTest extends CacheContractTest {

	@Override
	protected Cache<String, String> newCache() {
		return Hotshelf.<String, String>builder().build();
	}

	@Test
	void eachBuildReturnsAnIndependentCache() {
		var builder = Hotshelf.<String, String>builder().maximumSize(10);
		Cache<String, String> first = builder.build();
		Cache<String, String> second = builder.build();

		first.put("k", "v");

		assertThat(second).isNotSameAs(first);
		assertThat(second.get("k")).isEmpty();
	}

	/** A bounded cache from the builder must honour the contract too, while it has room. */
	@Nested
	class Bounded extends CacheContractTest {

		@Override
		protected Cache<String, String> newCache() {
			return Hotshelf.<String, String>builder().maximumSize(100).build();
		}

	}

}
