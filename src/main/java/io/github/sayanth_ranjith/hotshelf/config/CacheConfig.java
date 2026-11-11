package io.github.sayanth_ranjith.hotshelf.config;

import io.github.sayanth_ranjith.hotshelf.Cache;
import io.github.sayanth_ranjith.hotshelf.Hotshelf;
import io.github.sayanth_ranjith.hotshelf.eviction.EvictionPolicyType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires caches as Spring beans. The cache classes stay plain Java; Spring only creates them and hands them out.
 */
@Configuration
public class CacheConfig {

	/** An example bounded LRU cache. Inject it with {@code Cache<String, String>}. */
	@Bean
	public Cache<String, String> stringCache() {
		return Hotshelf.<String, String>builder()
				.maximumSize(1_000)
				.evictionPolicy(EvictionPolicyType.LRU)
				.build();
	}

}
