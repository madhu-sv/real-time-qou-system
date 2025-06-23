package com.madhu.qou.service.rewriting;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.madhu.qou.dto.UnderstoodQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class QueryRewriteService {

    // <-- FIX: Define the index name here
    private final String indexName = "products_index";

    private static final Map<String, String> SYNONYM_MAP = Map.of("shoes", "footwear");

    public SearchRequest buildEsQuery(UnderstoodQuery understoodQuery) {
        final String queryText = understoodQuery.preprocessedQuery().normalizedQuery();
        final var entities = understoodQuery.entities();
        final var userContext = understoodQuery.preprocessedQuery().userContext();

        // 1. Build "must" clause for main text query
        Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(queryText)
                .fields("name", "description", "brand", "search_aid^0.5")
                .type(TextQueryType.BestFields)
        )._toQuery();

        // 2. Build "filter" clauses from extracted entities
        List<Query> filterClauses = new ArrayList<>();
        for (var entity : entities) {
            switch (entity.type()) {
                case "BRAND" ->
                        filterClauses.add(TermQuery.of(t -> t.field("brand.keyword").value(entity.value()))._toQuery());
                case "ATTRIBUTE_COLOR" ->
                        filterClauses.add(NestedQuery.of(n -> n
                                .path("attributes")
                                .query(q -> q
                                        .bool(b -> b
                                                .must(
                                                        MatchQuery.of(mq -> mq.field("attributes.name").query("color"))._toQuery(),
                                                        MatchQuery.of(mq -> mq.field("attributes.value").query(entity.value()))._toQuery()
                                                )
                                        )
                                )
                        )._toQuery());
                case "GROCERY_ATTRIBUTE" -> {
                    if ("organic".equals(entity.value())) {
                        filterClauses.add(TermQuery.of(t -> t.field("grocery_attributes.is_organic").value(true))._toQuery());
                    }
                }
            }
        }

        // 3. Build "should" clauses for boosting
        List<Query> shouldClauses = new ArrayList<>();
        SYNONYM_MAP.forEach((key, value) -> {
            if (queryText.contains(key)) {
                shouldClauses.add(MultiMatchQuery.of(m -> m.query(value).fields("name", "search_aid"))._toQuery());
            }
        });

        // 4. Assemble final Bool Query
        BoolQuery boolQuery = BoolQuery.of(b -> b
                .must(multiMatchQuery)
                .filter(filterClauses)
                .should(shouldClauses)
        );

        // 5. THIS IS THE NEW LOGGING STEP - Let's see the REAL query
        log.info("Constructed ES Query DSL: {}", boolQuery.toString());

        // 6. Create the final SearchRequest object
        return SearchRequest.of(s -> s
                .index(indexName)
                .query(q -> q.bool(boolQuery))
        );
    }
}