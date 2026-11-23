package io.github.sayanth_ranjith.hotshelf.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sayanth_ranjith.hotshelf.Cache;
import io.github.sayanth_ranjith.hotshelf.Hotshelf;
import io.github.sayanth_ranjith.hotshelf.HotshelfBuilder;
import io.github.sayanth_ranjith.hotshelf.eviction.EvictionPolicyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A tour of every feature hotshelf has today, written as an outside application would use it: only the public API
 * ({@link Hotshelf}, {@link HotshelfBuilder}, {@link Cache}, {@link EvictionPolicyType}), never {@code LocalCache}.
 * <p>
 * Each nested class is one feature. Run them one at a time and step through them in the debugger. Good breakpoints:
 * <ul>
 * <li>{@code HotshelfBuilder.build} - which cache and policy get created</li>
 * <li>{@code LocalCache.put} / {@code LocalCache.get} - the map and the policy being updated together</li>
 * <li>{@code LocalCache.evictOne} and {@code LruEvictionPolicy.victim} - who gets dropped and why</li>
 * <li>{@code LruEvictionPolicy.recordAccess} - watch the key move to the end of the {@code order} map</li>
 * </ul>
 * Not covered yet because it isn't wired into the builder: TTL ({@code expireAfterWrite}, iteration 03),
 * thread safety (04), stats (05) and {@code get(key, loader)} (06).
 */
@DisplayName("hotshelf feature tour")
class FeatureTourTest {

	record Product(String sku, String name, long priceCents) {
	}

	@Nested
	@DisplayName("1. Building a cache")
	class Building {

		@Test
		@DisplayName("no options: an unbounded cache that never evicts")
		void unbounded() {
			Cache<Integer, String> cache = Hotshelf.<Integer, String>builder().build();

			for (int i = 0; i < 10_000; i++) {
				cache.put(i, "v" + i);
			}

			assertThat(cache.size()).isEqualTo(10_000);
			assertThat(cache.get(0)).contains("v0"); // the very first entry is still there
		}

		@Test
		@DisplayName("maximumSize alone: bounded, and the policy defaults to LRU")
		void boundedWithDefaultPolicy() {
			Cache<String, String> cache = Hotshelf.<String, String>builder()
					.maximumSize(2)
					.build();

			cache.put("a", "1");
			cache.put("b", "2");
			cache.put("c", "3"); // full: "a" is the least recently used, so it goes

			assertThat(cache.size()).isEqualTo(2);
			assertThat(cache.get("a")).isEmpty();
		}

		@Test
		@DisplayName("maximumSize + evictionPolicy(LRU): the same thing, said explicitly")
		void boundedWithExplicitPolicy() {
			Cache<String, String> cache = Hotshelf.<String, String>builder()
					.maximumSize(2)
					.evictionPolicy(EvictionPolicyType.LRU)
					.build();

			cache.put("a", "1");
			cache.put("b", "2");
			cache.put("c", "3");

			assertThat(cache.get("a")).isEmpty();
			assertThat(cache.get("b")).contains("2");
			assertThat(cache.get("c")).contains("3");
		}

		@Test
		@DisplayName("one builder, many caches: each build() is independent")
		void builderIsReusable() {
			HotshelfBuilder<String, String> builder = Hotshelf.<String, String>builder().maximumSize(10);

			Cache<String, String> sessions = builder.build();
			Cache<String, String> tokens = builder.build();
			sessions.put("k", "session");

			assertThat(sessions).isNotSameAs(tokens);
			assertThat(tokens.get("k")).isEmpty(); // no shared map, no shared LRU order
		}

		@Test
		@DisplayName("bad configuration fails fast, at build time, not at first use")
		void invalidConfiguration() {
			assertThatThrownBy(() -> Hotshelf.builder().maximumSize(0))
					.isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> Hotshelf.builder().maximumSize(-5))
					.isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> Hotshelf.builder().evictionPolicy(null))
					.isInstanceOf(NullPointerException.class);
			// An unbounded cache never evicts, so asking for a policy without a size is a mistake.
			assertThatThrownBy(() -> Hotshelf.builder().evictionPolicy(EvictionPolicyType.LRU).build())
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("maximumSize");
		}

	}

	@Nested
	@DisplayName("2. The basic operations: get, put, remove, clear, size")
	class BasicOperations {

		private final Cache<String, Product> cache = Hotshelf.<String, Product>builder().maximumSize(100).build();
		private final Product mug = new Product("SKU-1", "Mug", 899);

		@Test
		@DisplayName("put then get is a hit; an unknown key is a miss (Optional.empty, never null)")
		void hitAndMiss() {
			cache.put(mug.sku(), mug);

			assertThat(cache.get("SKU-1")).contains(mug);
			assertThat(cache.get("SKU-404")).isEmpty();
		}

		@Test
		@DisplayName("put on an existing key replaces the value; size stays the same")
		void overwrite() {
			cache.put("SKU-1", mug);
			cache.put("SKU-1", new Product("SKU-1", "Mug", 699)); // price drop

			assertThat(cache.size()).isEqualTo(1);
			assertThat(cache.get("SKU-1")).map(Product::priceCents).contains(699L);
		}

		@Test
		@DisplayName("remove deletes one entry; removing a missing key is a harmless no-op")
		void remove() {
			cache.put("SKU-1", mug);
			cache.remove("SKU-1");
			cache.remove("SKU-1"); // again: nothing happens, no exception

			assertThat(cache.get("SKU-1")).isEmpty();
			assertThat(cache.size()).isZero();
		}

		@Test
		@DisplayName("clear empties everything, and the cache is fully usable afterwards")
		void clear() {
			cache.put("SKU-1", mug);
			cache.put("SKU-2", new Product("SKU-2", "Plate", 1299));

			cache.clear();
			assertThat(cache.size()).isZero();

			cache.put("SKU-3", new Product("SKU-3", "Bowl", 999));
			assertThat(cache.size()).isEqualTo(1);
		}

	}

	@Nested
	@DisplayName("3. Working with Optional results")
	class OptionalIdioms {

		private final Cache<String, Product> cache = Hotshelf.<String, Product>builder().build();

		@Test
		@DisplayName("the common ways callers consume a get()")
		void idioms() {
			cache.put("SKU-1", new Product("SKU-1", "Mug", 899));

			// Default value on a miss.
			String name = cache.get("SKU-404").map(Product::name).orElse("unknown");
			assertThat(name).isEqualTo("unknown");

			// Transform a hit without unwrapping it.
			Optional<Long> price = cache.get("SKU-1").map(Product::priceCents);
			assertThat(price).contains(899L);

			// Branch on hit vs miss.
			List<String> log = new ArrayList<>();
			cache.get("SKU-1").ifPresentOrElse(p -> log.add("hit " + p.sku()), () -> log.add("miss"));
			cache.get("SKU-2").ifPresentOrElse(p -> log.add("hit " + p.sku()), () -> log.add("miss"));
			assertThat(log).containsExactly("hit SKU-1", "miss");

			// Turn a miss into an exception, e.g. in a "must exist" code path.
			assertThatThrownBy(() -> cache.get("SKU-404").orElseThrow())
					.isInstanceOf(java.util.NoSuchElementException.class);
		}

	}

	@Nested
	@DisplayName("4. Nulls are rejected")
	class Nulls {

		private final Cache<String, String> cache = Hotshelf.<String, String>builder().build();

		@Test
		@DisplayName("null keys and values throw NullPointerException; absence is Optional, not null")
		void nullsThrow() {
			assertThatThrownBy(() -> cache.get(null)).isInstanceOf(NullPointerException.class);
			assertThatThrownBy(() -> cache.put(null, "v")).isInstanceOf(NullPointerException.class);
			assertThatThrownBy(() -> cache.put("k", null)).isInstanceOf(NullPointerException.class);
			assertThatThrownBy(() -> cache.remove(null)).isInstanceOf(NullPointerException.class);
		}

		@Test
		@DisplayName("so to cache 'this row does not exist', store an explicit marker instead of null")
		void cachingAbsence() {
			Cache<String, Optional<String>> lookups = Hotshelf.<String, Optional<String>>builder().build();

			lookups.put("ghost", Optional.empty()); // "we already checked: no such row"

			assertThat(lookups.get("ghost")).contains(Optional.empty()); // hit, whose value says "absent"
			assertThat(lookups.get("never-asked")).isEmpty(); // a real miss
		}

	}

	@Nested
	@DisplayName("5. LRU eviction, step by step")
	class LruEviction {

		private final Cache<String, String> cache = Hotshelf.<String, String>builder()
				.maximumSize(3)
				.evictionPolicy(EvictionPolicyType.LRU)
				.build();

		@Test
		@DisplayName("with no reads, eviction goes in insertion order")
		void insertionOrderWhenUntouched() {
			cache.put("a", "1");
			cache.put("b", "2");
			cache.put("c", "3"); // LRU order (oldest first): a, b, c

			cache.put("d", "4"); // evicts a -> b, c, d
			cache.put("e", "5"); // evicts b -> c, d, e

			assertThat(cache.size()).isEqualTo(3);
			assertThat(cache.get("a")).isEmpty();
			assertThat(cache.get("b")).isEmpty();
		}

		@Test
		@DisplayName("a get() moves the key to most recently used, so it survives")
		void readRefreshes() {
			cache.put("a", "1");
			cache.put("b", "2");
			cache.put("c", "3"); // a, b, c

			cache.get("a"); // b, c, a   <- a read is a write to the LRU order (remember this for iteration 04)
			cache.put("d", "4"); // evicts b -> c, a, d

			assertThat(cache.get("a")).contains("1");
			assertThat(cache.get("b")).isEmpty();
		}

		@Test
		@DisplayName("overwriting a key with put() also counts as a use")
		void overwriteRefreshes() {
			cache.put("a", "1");
			cache.put("b", "2");
			cache.put("c", "3"); // a, b, c

			cache.put("a", "1-updated"); // b, c, a
			cache.put("d", "4"); // evicts b

			assertThat(cache.get("a")).contains("1-updated");
			assertThat(cache.get("b")).isEmpty();
		}

		@Test
		@DisplayName("a miss does not change the order")
		void missDoesNotRefresh() {
			cache.put("a", "1");
			cache.put("b", "2");
			cache.put("c", "3");

			cache.get("zzz"); // miss: nothing to move
			cache.put("d", "4");

			assertThat(cache.get("a")).isEmpty(); // a was still the oldest
		}

		@Test
		@DisplayName("remove frees a slot, so the next new key does not evict anyone")
		void removeFreesSlot() {
			cache.put("a", "1");
			cache.put("b", "2");
			cache.put("c", "3");

			cache.remove("b");
			cache.put("d", "4");

			assertThat(cache.size()).isEqualTo(3);
			assertThat(cache.get("a")).contains("1"); // not evicted: there was room
		}

		@Test
		@DisplayName("maximumSize(1): every new key replaces the previous one")
		void sizeOne() {
			Cache<String, String> single = Hotshelf.<String, String>builder().maximumSize(1).build();

			single.put("a", "1");
			single.put("b", "2");

			assertThat(single.size()).isEqualTo(1);
			assertThat(single.get("a")).isEmpty();
			assertThat(single.get("b")).contains("2");
		}

	}

	@Nested
	@DisplayName("6. LRU in practice: what it is good and bad at")
	class LruInPractice {

		@Test
		@DisplayName("strength: a hot key that keeps being read survives a stream of one-off keys")
		void hotKeySurvives() {
			Cache<String, String> cache = Hotshelf.<String, String>builder().maximumSize(3).build();
			cache.put("homepage", "<html>");

			for (int i = 0; i < 1_000; i++) {
				cache.get("homepage"); // every request also hits the homepage
				cache.put("product-" + i, "p" + i);
			}

			assertThat(cache.get("homepage")).contains("<html>");
			assertThat(cache.size()).isEqualTo(3);
		}

		@Test
		@DisplayName("weakness: one big scan flushes out the whole working set (why LFU arrives in v2)")
		void scanPollution() {
			Cache<String, String> cache = Hotshelf.<String, String>builder().maximumSize(3).build();
			cache.put("hot-1", "x");
			cache.put("hot-2", "x");
			cache.put("hot-3", "x");
			for (int round = 0; round < 100; round++) {
				cache.get("hot-1");
				cache.get("hot-2");
				cache.get("hot-3"); // read 100 times each, but LRU only remembers *when*, not *how often*
			}

			// A nightly report reads three rows once each.
			cache.put("report-row-1", "r");
			cache.put("report-row-2", "r");
			cache.put("report-row-3", "r");

			assertThat(cache.get("hot-1")).isEmpty();
			assertThat(cache.get("hot-2")).isEmpty();
			assertThat(cache.get("hot-3")).isEmpty();
		}

	}

	@Nested
	@DisplayName("7. A real service: read-through on find, write-through on update, invalidate on delete")
	class ProductService {

		/** Stands in for a database table, counting reads so the test can tell hits from misses. */
		static final class ProductRepository {

			private final Map<String, Product> rows = new HashMap<>();
			int reads;
			int writes;

			Optional<Product> findBySku(String sku) {
				reads++;
				return Optional.ofNullable(rows.get(sku));
			}

			void save(Product product) {
				writes++;
				rows.put(product.sku(), product);
			}

			void delete(String sku) {
				writes++;
				rows.remove(sku);
			}

		}

		/** The application class. It only knows {@link Cache}, so the implementation behind it can change freely. */
		static final class CatalogService {

			private final ProductRepository repository;
			private final Cache<String, Product> cache;
			int hits;
			int misses;

			CatalogService(ProductRepository repository, Cache<String, Product> cache) {
				this.repository = repository;
				this.cache = cache;
			}

			/** Cache-aside read. Until iteration 05 adds stats, the service counts hits and misses itself. */
			Optional<Product> find(String sku) {
				Optional<Product> cached = cache.get(sku);
				if (cached.isPresent()) {
					hits++;
					return cached;
				}
				misses++;
				Optional<Product> loaded = repository.findBySku(sku);
				loaded.ifPresent(p -> cache.put(sku, p)); // don't cache "not found" here (see Nulls#cachingAbsence)
				return loaded;
			}

			/** Write-through: update the source of truth first, then the cache, so readers see the new value at once. */
			void updatePrice(String sku, long newPriceCents) {
				Product current = find(sku).orElseThrow();
				Product updated = new Product(sku, current.name(), newPriceCents);
				repository.save(updated);
				cache.put(sku, updated);
			}

			/** Invalidate: after a delete, the cache must not keep serving the old row. */
			void discontinue(String sku) {
				repository.delete(sku);
				cache.remove(sku);
			}

			double hitRate() {
				int total = hits + misses;
				return total == 0 ? 0 : (double) hits / total;
			}

		}

		@Test
		@DisplayName("the full lifecycle of a product through the cache")
		void lifecycle() {
			ProductRepository repository = new ProductRepository();
			repository.save(new Product("SKU-1", "Mug", 899));
			repository.save(new Product("SKU-2", "Plate", 1299));
			repository.save(new Product("SKU-3", "Bowl", 999));
			repository.writes = 0;

			Cache<String, Product> cache = Hotshelf.<String, Product>builder().maximumSize(2).build();
			CatalogService catalog = new CatalogService(repository, cache);

			// Cold start: misses load from the repository.
			catalog.find("SKU-1");
			catalog.find("SKU-2");
			assertThat(repository.reads).isEqualTo(2);

			// Warm: hits never touch the repository.
			catalog.find("SKU-1");
			catalog.find("SKU-2");
			catalog.find("SKU-1");
			assertThat(repository.reads).isEqualTo(2);
			assertThat(catalog.hits).isEqualTo(3); // LRU order now: SKU-2, SKU-1

			// A third product on a size-2 cache evicts the least recently used (SKU-2).
			catalog.find("SKU-3");
			assertThat(cache.get("SKU-2")).isEmpty();
			assertThat(cache.get("SKU-1")).isPresent();

			// Write-through: the cache holds the new price immediately, no extra repository read.
			int readsBefore = repository.reads;
			catalog.updatePrice("SKU-1", 499);
			assertThat(catalog.find("SKU-1")).map(Product::priceCents).contains(499L);
			assertThat(repository.reads).isEqualTo(readsBefore);

			// Invalidate: the deleted product is gone from both places.
			catalog.discontinue("SKU-1");
			assertThat(cache.get("SKU-1")).isEmpty();
			assertThat(catalog.find("SKU-1")).isEmpty(); // miss, repository says not found, nothing cached

			// A miss for a product that doesn't exist is not cached, so each lookup goes to the repository.
			int readsBeforeGhost = repository.reads;
			catalog.find("SKU-404");
			catalog.find("SKU-404");
			assertThat(repository.reads).isEqualTo(readsBeforeGhost + 2);

			assertThat(catalog.hitRate()).isBetween(0.0, 1.0);
		}

	}

}
