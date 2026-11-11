package io.github.sayanth_ranjith.hotshelf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.sayanth_ranjith.hotshelf.eviction.EvictionPolicyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Builder options and validation, tested only through the public API.
 */
class HotshelfBuilderTest {

	private final HotshelfBuilder<String, String> builder = Hotshelf.builder();

	// --- validation ---

	@ParameterizedTest
	@ValueSource(longs = {0, -1, Long.MIN_VALUE})
	void maximumSizeRejectsNonPositiveValues(long size) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> builder.maximumSize(size))
				.withMessageContaining("maximumSize must be positive");
	}

	@Test
	void evictionPolicyRejectsNull() {
		assertThatNullPointerException().isThrownBy(() -> builder.evictionPolicy(null)).withMessage("evictionPolicy");
	}

	@Test
	void evictionPolicyWithoutMaximumSizeIsRejectedAtBuild() {
		builder.evictionPolicy(EvictionPolicyType.LRU);

		assertThatIllegalStateException().isThrownBy(builder::build).withMessageContaining("requires maximumSize");
	}

	// --- unbounded ---

	@Test
	void withoutMaximumSizeTheCacheIsUnbounded() {
		Cache<String, String> cache = builder.build();

		for (int i = 0; i < 10_000; i++) {
			cache.put("k" + i, "v");
		}

		assertThat(cache.size()).isEqualTo(10_000);
		assertThat(cache.get("k0")).contains("v");
	}

	// --- bounded, LRU end to end ---

	@Test
	void lruEvictsTheLeastRecentlyUsedEntry() {
		Cache<String, String> cache = builder.maximumSize(3).evictionPolicy(EvictionPolicyType.LRU).build();
		cache.put("A", "1");
		cache.put("B", "2");
		cache.put("C", "3");

		cache.get("A");
		cache.put("D", "4");

		assertThat(cache.get("B")).isEmpty();
		assertThat(cache.get("A")).contains("1");
		assertThat(cache.get("C")).contains("3");
		assertThat(cache.get("D")).contains("4");
		assertThat(cache.size()).isEqualTo(3);
	}

	@Test
	void maximumSizeAloneDefaultsToLru() {
		Cache<String, String> cache = builder.maximumSize(3).build();
		cache.put("A", "1");
		cache.put("B", "2");
		cache.put("C", "3");

		cache.get("A");
		cache.put("D", "4");

		assertThat(cache.get("B")).isEmpty();
		assertThat(cache.get("A")).contains("1");
	}

	@Test
	void sizeNeverExceedsMaximumSize() {
		Cache<String, String> cache = builder.maximumSize(5).build();

		for (int i = 0; i < 100; i++) {
			cache.put("k" + i, "v");
			assertThat(cache.size()).isLessThanOrEqualTo(5);
		}
	}

}
