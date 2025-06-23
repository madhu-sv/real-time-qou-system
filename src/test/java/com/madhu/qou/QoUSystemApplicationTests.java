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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
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

	// Inject the DataSeeder so we can control it
	@Autowired
	private DataSeeder dataSeeder;

	// This method will run BEFORE each @Test, guaranteeing data is ready
	@BeforeEach
	void setUp() throws Exception {
		// Manually trigger the data seeding
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
				.andExpect(jsonPath("$.facets", hasSize(greaterThan(0))))
				.andExpect(jsonPath("$.facets[?(@.name == 'Category')].values", hasSize(greaterThan(0))))
				.andExpect(jsonPath("$.facets[?(@.name == 'Brand')].values", hasSize(greaterThan(0))));
	}
}