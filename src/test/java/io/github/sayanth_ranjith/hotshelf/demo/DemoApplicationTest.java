package io.github.sayanth_ranjith.hotshelf.demo;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sayanth_ranjith.hotshelf.Cache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** A Spring app can create a hotshelf cache as a bean and inject it. */
@SpringBootTest
class DemoApplicationTest {

	@Autowired
	private Cache<String, String> stringCache;

	@Test
	void cacheBeanIsInjectedAndWorks() {
		stringCache.put("k", "v");

		assertThat(stringCache.get("k")).contains("v");
	}

}
