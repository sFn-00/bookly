package com.bookly;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
@Disabled("Requires running PostgreSQL — covered by Testcontainers tests in Task 14")
class BooklyApplicationTests {

	@Test
	void contextLoads() {
	}
}
