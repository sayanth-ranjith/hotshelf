package io.github.sayanth_ranjith.hotshelf.eviction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EvictionPolicyFactoryTest {

	@Test
	void lruCreatesAnLruPolicy() {
		assertThat(EvictionPolicyFactory.<String>create(EvictionPolicyType.LRU)).isInstanceOf(LruEvictionPolicy.class);
	}

	@ParameterizedTest
	@EnumSource(EvictionPolicyType.class)
	void everyTypeCreatesAFreshEmptyPolicy(EvictionPolicyType type) {
		EvictionPolicy<String> first = EvictionPolicyFactory.create(type);
		EvictionPolicy<String> second = EvictionPolicyFactory.create(type);

		assertThat(first).isNotSameAs(second);
		assertThat(first.victim()).isEmpty();
	}

	@Test
	void rejectsNullType() {
		assertThatNullPointerException().isThrownBy(() -> EvictionPolicyFactory.create(null)).withMessage("type");
	}

}
