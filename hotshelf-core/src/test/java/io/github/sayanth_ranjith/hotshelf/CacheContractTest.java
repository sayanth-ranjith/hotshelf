package io.github.sayanth_ranjith.hotshelf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The behaviour every {@link Cache} must have. Each implementation gets a subclass that only supplies
 * {@link #newCache()} and inherits every test.
 */
public abstract class CacheContractTest {

	private Cache<String, String> cache;

	/** Returns a new, empty cache. Called once per test. */
	protected abstract Cache<String, String> newCache();

	@BeforeEach
	void createCache() {
		cache = newCache();
	}

	// --- hit and miss ---

	@Test
	void newCacheIsEmpty() {
		assertThat(cache.size()).isZero();
	}

	@Test
	void getMissingKeyIsEmpty() {
		assertThat(cache.get("missing")).isEmpty();
	}

	@Test
	void getAfterPutReturnsValue() {
		cache.put("k", "v");

		assertThat(cache.get("k")).contains("v");
	}

	@Test
	void keysAreMatchedByEqualsNotIdentity() {
		cache.put(new String("k"), "v");

		assertThat(cache.get(new String("k"))).contains("v");
	}

	// --- put ---

	@Test
	void sizeCountsDistinctKeys() {
		cache.put("a", "1");
		cache.put("b", "2");

		assertThat(cache.size()).isEqualTo(2);
	}

	@Test
	void putOverwritesExistingValue() {
		cache.put("k", "old");
		cache.put("k", "new");

		assertThat(cache.get("k")).contains("new");
	}

	@Test
	void overwriteDoesNotChangeSize() {
		cache.put("k", "old");
		cache.put("k", "new");

		assertThat(cache.size()).isEqualTo(1);
	}

	// --- remove ---

	@Test
	void removeMakesKeyAbsent() {
		cache.put("k", "v");

		cache.remove("k");

		assertThat(cache.get("k")).isEmpty();
		assertThat(cache.size()).isZero();
	}

	@Test
	void removeLeavesOtherKeysAlone() {
		cache.put("a", "1");
		cache.put("b", "2");

		cache.remove("a");

		assertThat(cache.get("b")).contains("2");
		assertThat(cache.size()).isEqualTo(1);
	}

	@Test
	void removeMissingKeyIsNoOp() {
		cache.put("k", "v");

		cache.remove("missing");

		assertThat(cache.get("k")).contains("v");
		assertThat(cache.size()).isEqualTo(1);
	}

	// --- clear ---

	@Test
	void clearRemovesAllEntries() {
		cache.put("a", "1");
		cache.put("b", "2");

		cache.clear();

		assertThat(cache.size()).isZero();
		assertThat(cache.get("a")).isEmpty();
		assertThat(cache.get("b")).isEmpty();
	}

	@Test
	void cacheIsUsableAfterClear() {
		cache.put("a", "1");
		cache.clear();

		cache.put("a", "2");

		assertThat(cache.get("a")).contains("2");
	}

	// --- nulls are rejected (ADR-002) ---

	@Test
	void getRejectsNullKey() {
		assertThatNullPointerException().isThrownBy(() -> cache.get(null)).withMessage("key");
	}

	@Test
	void putRejectsNullKey() {
		assertThatNullPointerException().isThrownBy(() -> cache.put(null, "v")).withMessage("key");
	}

	@Test
	void putRejectsNullValue() {
		assertThatNullPointerException().isThrownBy(() -> cache.put("k", null)).withMessage("value");
	}

	@Test
	void rejectedPutLeavesExistingValueUntouched() {
		cache.put("k", "v");

		assertThatNullPointerException().isThrownBy(() -> cache.put("k", null));

		assertThat(cache.get("k")).contains("v");
	}

	@Test
	void removeRejectsNullKey() {
		assertThatNullPointerException().isThrownBy(() -> cache.remove(null)).withMessage("key");
	}

}
