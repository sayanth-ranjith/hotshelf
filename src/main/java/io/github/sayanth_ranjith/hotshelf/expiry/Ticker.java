package io.github.sayanth_ranjith.hotshelf.expiry;

/**
 * A source of time in nanoseconds.
 * <p>
 * The value has an arbitrary origin, so only the difference between two reads means anything. It must never go
 * backwards. Caches read time through this interface instead of calling {@link System#nanoTime()} directly, so tests
 * can move time by hand.
 */
@FunctionalInterface
public interface Ticker {

	/** Nanoseconds since an arbitrary, fixed origin. */
	long read();

	/** A ticker backed by {@link System#nanoTime()}. */
	static Ticker system() {
		return System::nanoTime;
	}

}
