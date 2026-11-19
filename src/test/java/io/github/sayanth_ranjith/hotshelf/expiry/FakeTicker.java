package io.github.sayanth_ranjith.hotshelf.expiry;

import java.time.Duration;
import java.util.Objects;

/** A {@link Ticker} that starts at 0 and only moves when a test calls {@link #advance}. */
public final class FakeTicker implements Ticker {

	private long nanos;

	@Override
	public long read() {
		return nanos;
	}

	/** Moves time forward. Negative durations are rejected because a ticker never goes backwards. */
	public FakeTicker advance(Duration duration) {
		Objects.requireNonNull(duration, "duration");
		if (duration.isNegative()) {
			throw new IllegalArgumentException("time cannot go backwards: " + duration);
		}
		nanos += duration.toNanos();
		return this;
	}

}
