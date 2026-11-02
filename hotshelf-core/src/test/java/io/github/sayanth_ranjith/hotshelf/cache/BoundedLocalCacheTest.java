package io.github.sayanth_ranjith.hotshelf.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Bounding behaviour of {@link LocalCache}, driven by a fake policy so only the cache's own logic is under test.
 */
class BoundedLocalCacheTest {

	private InsertionOrderPolicy<String> policy;
	private LocalCache<String, String> cache;

	@BeforeEach
	void createCache() {
		policy = new InsertionOrderPolicy<>();
		cache = new LocalCache<>(3, policy);
	}

	// --- eviction ---

	@Test
	void puttingMaxPlusOneEvictsThePolicysVictim() {
		cache.put("a", "1");
		cache.put("b", "2");
		cache.put("c", "3");

		cache.put("d", "4");

		assertThat(cache.get("a")).isEmpty();
		assertThat(cache.get("b")).contains("2");
		assertThat(cache.get("c")).contains("3");
		assertThat(cache.get("d")).contains("4");
	}

	@Test
	void sizeNeverExceedsMaximum() {
		for (int i = 0; i < 10; i++) {
			cache.put("k" + i, "v" + i);
			assertThat(cache.size()).isLessThanOrEqualTo(3);
		}
		assertThat(cache.size()).isEqualTo(3);
	}

	@Test
	void evictionHappensBeforeTheNewKeyIsInserted() {
		cache.put("a", "1");
		cache.put("b", "2");
		cache.put("c", "3");

		cache.put("d", "4");

		assertThat(policy.calls()).endsWith("removal:a", "insert:d");
	}

	@Test
	void maximumSizeOfOneKeepsOnlyTheLatestKey() {
		LocalCache<String, String> single = new LocalCache<>(1, new InsertionOrderPolicy<>());

		single.put("a", "1");
		single.put("b", "2");

		assertThat(single.get("a")).isEmpty();
		assertThat(single.get("b")).contains("2");
		assertThat(single.size()).isEqualTo(1);
	}

	// --- updates and removals ---

	@Test
	void updatingAKeyOnAFullCacheDoesNotEvict() {
		cache.put("a", "1");
		cache.put("b", "2");
		cache.put("c", "3");

		cache.put("a", "new");

		assertThat(cache.size()).isEqualTo(3);
		assertThat(cache.get("a")).contains("new");
		assertThat(cache.get("b")).contains("2");
		assertThat(cache.get("c")).contains("3");
	}

	@Test
	void updateIsReportedAsAnAccessNotAnInsert() {
		cache.put("a", "1");

		cache.put("a", "2");

		assertThat(policy.calls()).containsExactly("insert:a", "access:a");
	}

	@Test
	void removeFreesASlot() {
		cache.put("a", "1");
		cache.put("b", "2");
		cache.put("c", "3");
		cache.remove("b");

		cache.put("d", "4");

		assertThat(cache.size()).isEqualTo(3);
		assertThat(cache.get("a")).contains("1");
		assertThat(cache.get("c")).contains("3");
		assertThat(cache.get("d")).contains("4");
	}

	// --- cache and policy stay in sync ---

	@Test
	void getHitRecordsAnAccess() {
		cache.put("a", "1");

		cache.get("a");

		assertThat(policy.calls()).containsExactly("insert:a", "access:a");
	}

	@Test
	void getMissDoesNotTouchThePolicy() {
		cache.get("missing");

		assertThat(policy.calls()).isEmpty();
	}

	@Test
	void evictedKeyIsNoLongerTracked() {
		cache.put("a", "1");
		cache.put("b", "2");
		cache.put("c", "3");
		cache.put("d", "4");

		assertThat(policy.tracked()).containsExactly("b", "c", "d");
	}

	@Test
	void removedKeyIsNoLongerTracked() {
		cache.put("a", "1");
		cache.put("b", "2");

		cache.remove("a");

		assertThat(policy.tracked()).containsExactly("b");
	}

	@Test
	void removingAMissingKeyDoesNotTouchThePolicy() {
		cache.remove("missing");

		assertThat(policy.calls()).isEmpty();
	}

	@Test
	void clearAlsoClearsThePolicy() {
		cache.put("a", "1");
		cache.put("b", "2");

		cache.clear();

		assertThat(policy.tracked()).isEmpty();
	}

	// --- constructor validation ---

	@ParameterizedTest
	@ValueSource(longs = {0, -1, Long.MIN_VALUE})
	void rejectsNonPositiveMaximumSize(long maximumSize) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new LocalCache<String, String>(maximumSize, new InsertionOrderPolicy<>()));
	}

	@Test
	void rejectsNullPolicy() {
		assertThatNullPointerException().isThrownBy(() -> new LocalCache<String, String>(3, null));
	}

}
