package com.madhu.qou.service.rewriting;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.madhu.qou.dto.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueryRewriteServiceTest {

    private final QueryRewriteService queryRewriteService = new QueryRewriteService();

    @Test
    void whenQueryContainsOrganic_thenQueryStringShouldContainFilter() {
        // Arrange: Create the input data our service method needs
        PreprocessedQuery preprocessedQuery = new PreprocessedQuery(
                "organic avocados",
                "organic avocados",
                new UserContext("user-123", "vip")
        );
        Entity organicEntity = new Entity("organic", "GROCERY_ATTRIBUTE", 0, 7);
        UnderstoodQuery understoodQuery = new UnderstoodQuery(
                preprocessedQuery,
                new Intent("find_product"),
                List.of(organicEntity)
        );

        // Act: Call the method directly
        SearchRequest result = queryRewriteService.buildEsQuery(understoodQuery);

        // Assert: Check the .toString() output of the query for key components.
        // This is more robust than checking for one large, exact string.
        String queryAsString = result.query().toString();

        // Check that the key parts of the query exist in the string representation
        assertThat(queryAsString).contains("multi_match");
        assertThat(queryAsString).contains("organic avocados");
        assertThat(queryAsString).contains("grocery_attributes.is_organic");
        assertThat(queryAsString).contains("\"value\":true");
    }
}