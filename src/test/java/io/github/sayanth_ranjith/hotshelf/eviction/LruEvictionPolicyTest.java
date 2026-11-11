package io.github.sayanth_ranjith.hotshelf.eviction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LruEvictionPolicyTest {

	private LruEvictionPolicy<String> policy;

	@BeforeEach
	void createPolicy() {
		policy = new LruEvictionPolicy<>();
	}

	// --- insert and victim ---

	@Test
	void emptyPolicyHasNoVictim() {
		assertThat(policy.victim()).isEmpty();
	}

	@Test
	void victimIsTheOldestInsertedKey() {
		policy.recordInsert("a");
		policy.recordInsert("b");
		policy.recordInsert("c");

		assertThat(policy.victim()).contains("a");
	}

	@Test
	void victimDoesNotChangeState() {
		policy.recordInsert("a");
		policy.recordInsert("b");

		policy.victim();

		assertThat(policy.victim()).contains("a");
	}

	@Test
	void insertingATrackedKeyAgainActsLikeAnAccess() {
		policy.recordInsert("a");
		policy.recordInsert("b");

		policy.recordInsert("a");

		assertThat(policy.victim()).contains("b");
		policy.recordRemoval("b");
		policy.recordRemoval("a");
		assertThat(policy.victim()).isEmpty();
	}

	// --- access ---

	@Test
	void accessMakesAKeyMostRecentlyUsed() {
		policy.recordInsert("a");
		policy.recordInsert("b");
		policy.recordInsert("c");

		policy.recordAccess("a");

		assertThat(policy.victim()).contains("b");
	}

	@Test
	void accessingAnUnknownKeyChangesNothing() {
		policy.recordInsert("a");

		policy.recordAccess("ghost");

		assertThat(policy.victim()).contains("a");
		policy.recordRemoval("a");
		assertThat(policy.victim()).isEmpty();
	}

	// --- removal and clear ---

	@Test
	void removedKeyIsNoLongerTheVictim() {
		policy.recordInsert("a");
		policy.recordInsert("b");

		policy.recordRemoval("a");

		assertThat(policy.victim()).contains("b");
	}

	@Test
	void removingTheOnlyKeyLeavesNoVictim() {
		policy.recordInsert("a");

		policy.recordRemoval("a");

		assertThat(policy.victim()).isEmpty();
	}

	@Test
	void removingAnUnknownKeyChangesNothing() {
		policy.recordInsert("a");

		policy.recordRemoval("ghost");

		assertThat(policy.victim()).contains("a");
	}

	@Test
	void clearForgetsEveryKeyAndThePolicyStillWorks() {
		policy.recordInsert("a");
		policy.recordInsert("b");

		policy.clear();

		assertThat(policy.victim()).isEmpty();
		policy.recordInsert("c");
		assertThat(policy.victim()).contains("c");
	}

	// --- nulls ---

	@Test
	void everyKeyMethodRejectsNull() {
		assertThatNullPointerException().isThrownBy(() -> policy.recordInsert(null));
		assertThatNullPointerException().isThrownBy(() -> policy.recordAccess(null));
		assertThatNullPointerException().isThrownBy(() -> policy.recordRemoval(null));
	}

}
