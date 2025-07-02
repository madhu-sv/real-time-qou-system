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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.Duration;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class QoUSystemApplicationTests {

    @Container
    static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            "docker.elastic.co/elasticsearch/elasticsearch:8.14.0")
            .withEnv("xpack.security.enabled", "false");

    @Container
    static final GenericContainer<?> nerApi = new GenericContainer<>("real-time-qou-system-ner-api:latest")
            .withExposedPorts(8000)
            .waitingFor(
                Wait.forHttp("/ent")
                    .forStatusCodeMatching(status -> status == 200 || status == 405)
                    .withStartupTimeout(Duration.ofSeconds(120))
            );

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress);
        registry.add("ner.api.url", () -> String.format(
                "http://%s:%d/ent", nerApi.getHost(), nerApi.getMappedPort(8000)));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() throws Exception {
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

    @Test
    void whenQueryIsMispelled_thenReturnsDidYouMeanSuggestion() throws Exception {
        String requestBody = """
            {
                "rawQuery": "organc avocodo"
            }
            """;

        mockMvc.perform(post("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(0)))
                .andExpect(jsonPath("$.didYouMeanSuggestion", is("organic avocado")));
    }

    // --- NEW AND IMPROVED TEST FOR THE NER FEATURE ---
    @Test
    void whenQueryContainsBrandEntity_thenResultsAreFilteredByBrand() throws Exception {
        // Arrange: Use a query that our NER model will parse to find a BRAND
        String requestBody = """
            {
                "rawQuery": "show me products from Philadelphia"
            }
            """;

        // Act & Assert: Hit the main /search endpoint
        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                // 1. Assert we actually get some products back
                .andExpect(jsonPath("$.products", hasSize(greaterThan(0))))
                // 2. This is the crucial assertion:
                //    It checks that in the list of results, there are NO products
                //    where the brand is NOT 'Horizon Organic'. This proves the filter worked.
                .andExpect(jsonPath("$.products[?(@.brand != 'Philadelphia')]", empty()));
    }

    @Test
    void whenQueryContainsAisleEntity_thenResultsAreFilteredByCategory() throws Exception {
        // Arrange: Use a query that our NER model will parse to find an AISLE
        // The pattern "yogurt" was added to patterns.json by our build script.
        String requestBody = """
            {
                "rawQuery": "show me items from the yogurt aisle"
            }
            """;

        // Act & Assert: Hit the main /search endpoint
        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                // 1. Assert we actually get some products back
                .andExpect(jsonPath("$.products", hasSize(greaterThan(0))))
                // 2. The crucial assertion:
                //    Assert that there are NO products in the results where the 'categories'
                //    array does NOT contain "yogurt". This proves the filter worked.
                .andExpect(jsonPath("$.products[?('yogurt' nin @.categories)]", empty()));
    }

    @Test
    void whenQueryContainsUnseenBrandEntity_thenResultsAreFilteredByBrand() throws Exception {
        // Arrange: Use a query with a brand not in patterns.json
        String requestBody = """
            {
                "rawQuery": "I want to buy some Nike shoes"
            }
            """;

        // Act & Assert
        mockMvc.perform(post("/api/v1/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.products[?(@.brand != 'Nike')]", empty()));
    }
}