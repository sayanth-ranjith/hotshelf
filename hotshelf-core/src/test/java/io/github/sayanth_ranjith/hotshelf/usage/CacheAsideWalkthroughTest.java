package io.github.sayanth_ranjith.hotshelf.usage;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sayanth_ranjith.hotshelf.cache.Cache;
import io.github.sayanth_ranjith.hotshelf.internal.eviction.EvictionPolicyType;
import io.github.sayanth_ranjith.hotshelf.cache.Hotshelf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * hotshelf used the way an application would use it: a service puts a cache in front of a slow repository
 * (cache-aside). This package is outside the library, so only the exported API compiles here.
 * <p>
 * Set breakpoints in {@code LocalCache.put}, {@code LocalCache.evictOne} and {@code LruEvictionPolicy.victim}
 * to watch each step from the inside.
 */
class CacheAsideWalkthroughTest {

	record User(String id, String name) {
	}

	/** Stands in for a database. Counts every read so the test can tell hits from misses. */
	static final class UserRepository {

		private final Map<String, User> rows = new HashMap<>();
		private int reads;

		UserRepository() {
			for (String id : new String[] {"u1", "u2", "u3", "u4"}) {
				rows.put(id, new User(id, "name-" + id));
			}
		}

		User findById(String id) {
			reads++;
			return rows.get(id);
		}

		void save(User user) {
			rows.put(user.id(), user);
		}

		int reads() {
			return reads;
		}

	}

	/** Cache-aside: check the cache, else load from the repository and remember the result. */
	static final class UserService {

		private final UserRepository repository;
		private final Cache<String, User> cache;

		UserService(UserRepository repository, Cache<String, User> cache) {
			this.repository = repository;
			this.cache = cache;
		}

		User find(String id) {
			return cache.get(id).orElseGet(() -> {
				User user = repository.findById(id);
				cache.put(id, user);
				return user;
			});
		}

		void rename(String id, String newName) {
			repository.save(new User(id, newName));
			cache.remove(id); // invalidate: the next find reloads the fresh row
		}

	}

	@Test
	@DisplayName("A service with an LRU cache of 3 users: hits, misses, eviction and invalidation")
	void fullFlow() {
		UserRepository repository = new UserRepository();
		Cache<String, User> cache = Hotshelf.<String, User>builder()
				.maximumSize(3)
				.evictionPolicy(EvictionPolicyType.LRU)
				.build();
		UserService service = new UserService(repository, cache);

		// 1. Cold cache: every first read is a miss and goes to the repository.
		service.find("u1");
		service.find("u2");
		service.find("u3");
		assertThat(repository.reads()).isEqualTo(3);
		assertThat(cache.size()).isEqualTo(3);
		// LRU order, oldest first: u1, u2, u3

		// 2. Warm read: a hit. The repository is not touched, and u1 becomes the most recently used.
		assertThat(service.find("u1").name()).isEqualTo("name-u1");
		assertThat(repository.reads()).isEqualTo(3);
		// LRU order: u2, u3, u1

		// 3. A new key on a full cache: the least recently used entry (u2) is evicted to make room.
		service.find("u4");
		assertThat(repository.reads()).isEqualTo(4);
		assertThat(cache.size()).isEqualTo(3);
		assertThat(cache.get("u2")).isEmpty();
		assertThat(cache.get("u1")).isPresent(); // survived because step 2 touched it
		// The two gets above count as accesses too. LRU order: u3, u4, u1

		// 4. The evicted user is a miss again, and loading it evicts the next oldest (u3).
		service.find("u2");
		assertThat(repository.reads()).isEqualTo(5);
		assertThat(cache.get("u3")).isEmpty();
		// LRU order: u4, u1, u2

		// 5. Invalidation: the row changes, the cached copy is removed, the next read loads the new name.
		service.rename("u1", "Sayanth");
		assertThat(cache.get("u1")).isEmpty();
		assertThat(cache.size()).isEqualTo(2);

		assertThat(service.find("u1").name()).isEqualTo("Sayanth");
		assertThat(repository.reads()).isEqualTo(6);
		assertThat(cache.size()).isEqualTo(3); // u1 filled the slot freed by remove, so nothing was evicted
		assertThat(cache.get("u4")).isPresent();
		assertThat(cache.get("u2")).isPresent();
	}

}
