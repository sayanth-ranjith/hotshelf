package io.github.sayanth_ranjith.hotshelf.demo;

import io.github.sayanth_ranjith.hotshelf.Cache;
import io.github.sayanth_ranjith.hotshelf.Hotshelf;
import io.github.sayanth_ranjith.hotshelf.eviction.EvictionPolicyType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * A tiny Spring Boot app that uses hotshelf the way a real application would: it depends on the library and
 * declares a cache bean itself. Lives in test sources, so it is never shipped in the hotshelf jar.
 */
@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	Cache<String, String> stringCache() {
		return Hotshelf.<String, String>builder()
				.maximumSize(1_000)
				.evictionPolicy(EvictionPolicyType.LRU)
				.build();
	}

}
