package com.madhu.qou;

import com.madhu.qou.service.DataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class QoUSystemApplicationTests {

	@Container
	private static final ElasticsearchContainer elasticsearchContainer =
			new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.14.0")
					.withEnv("xpack.security.enabled", "false");

	@DynamicPropertySource
	static void setProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DataSeeder dataSeeder;

	@BeforeEach
	void setUp() throws Exception {
		// This runs before EACH test, ensuring a clean, seeded database every time.
		dataSeeder.seedProductIndex();
		dataSeeder.seedSuggestionIndex();
	}

	@Test
	void whenSearchingForOrganic_thenReturnsProductsAndFacets() throws Exception {
		String requestBody = """
            {
                "rawQuery": "organic"
            }
            """;

		mockMvc.perform(post("/api/v1/search")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.products", hasSize(greaterThan(0))))
				.andExpect(jsonPath("$.facets", hasSize(greaterThan(0))));
	}

	// --- NEW TEST CASE FOR THE "DID YOU MEAN" FEATURE ---
	@Test
	void whenQueryIsMispelled_thenReturnsDidYouMeanSuggestion() throws Exception {
		// Arrange: Define a request with a clear typo
		String requestBody = """
            {
                "rawQuery": "organc avocodo"
            }
            """;

		// Act & Assert
		mockMvc.perform(post("/api/v1/search")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				// 1. Assert the HTTP status is 200 OK
				.andExpect(status().isOk())
				// 2. Assert that the products list is EMPTY, because the original query found nothing
				.andExpect(jsonPath("$.products", hasSize(0)))
				// 3. Assert that our new "didYouMeanSuggestion" field exists and has the corrected value
				.andExpect(jsonPath("$.didYouMeanSuggestion", is("organic avocado")));
	}
}