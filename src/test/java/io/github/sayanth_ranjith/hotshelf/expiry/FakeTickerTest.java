package io.github.sayanth_ranjith.hotshelf.expiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class FakeTickerTest {

	@Test
	void startsAtZero() {
		assertThat(new FakeTicker().read()).isZero();
	}

	@Test
	void advanceAddsUp() {
		FakeTicker ticker = new FakeTicker();

		ticker.advance(Duration.ofSeconds(1)).advance(Duration.ofNanos(5));

		assertThat(ticker.read()).isEqualTo(1_000_000_005L);
	}

	@Test
	void readDoesNotMoveTime() {
		FakeTicker ticker = new FakeTicker().advance(Duration.ofMinutes(10));

		assertThat(ticker.read()).isEqualTo(ticker.read());
	}

	@Test
	void rejectsNegativeDuration() {
		assertThatIllegalArgumentException().isThrownBy(() -> new FakeTicker().advance(Duration.ofNanos(-1)));
	}

	@Test
	void rejectsNullDuration() {
		assertThatNullPointerException().isThrownBy(() -> new FakeTicker().advance(null));
	}

	@Test
	void systemTickerDoesNotGoBackwards() {
		Ticker ticker = Ticker.system();

		long first = ticker.read();
		long second = ticker.read();

		assertThat(second - first).isGreaterThanOrEqualTo(0);
	}

}
