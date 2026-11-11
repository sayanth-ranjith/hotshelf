package io.github.sayanth_ranjith.hotshelf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HotshelfApplicationTest {

	@Autowired
	private Cache<String, String> stringCache;

	@Test
	void contextLoadsAndTheCacheBeanWorks() {
		stringCache.put("k", "v");

		assertThat(stringCache.get("k")).contains("v");
	}

}
