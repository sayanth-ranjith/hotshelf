package io.github.sayanth_ranjith.hotshelf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BuildSanityTest {

	@Test
	void runsOnJava21OrNewer() {
		assertThat(Runtime.version().feature()).isGreaterThanOrEqualTo(21);
	}

	@Test
	void junitAndAssertjAreWired() {
		assertThat(1 + 1).isEqualTo(2);
	}

}
