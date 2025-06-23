package com.madhu.qou.service.rewriting;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.madhu.qou.dto.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueryRewriteServiceTest {

    private final QueryRewriteService queryRewriteService = new QueryRewriteService();

    @Test
    void whenQueryContainsOrganic_thenQueryShouldContainFilterAndAggregations() {
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

        // Assert: Inspect the Java objects directly, which is more reliable than toString()

        // 1. Assert that the main query is a 'bool' query
        assertThat(result.query().isBool()).isTrue();

        // 2. Assert that the 'filter' part of the bool query has one clause
        List<Query> filters = result.query().bool().filter();
        assertThat(filters).hasSize(1);
        assertThat(filters.get(0).isTerm()).isTrue(); // Check that the filter is a term query
        assertThat(filters.get(0).term().field()).isEqualTo("grocery_attributes.is_organic");
        assertThat(filters.get(0).term().value().booleanValue()).isTrue();

        // 3. Assert that the 'must' part has our multi_match query
        assertThat(result.query().bool().must().get(0).isMultiMatch()).isTrue();
        assertThat(result.query().bool().must().get(0).multiMatch().query()).isEqualTo("organic avocados");

        // 4. Assert that aggregations have been added
        assertThat(result.aggregations()).containsKey("by_category");
        assertThat(result.aggregations()).containsKey("by_brand");
    }
}