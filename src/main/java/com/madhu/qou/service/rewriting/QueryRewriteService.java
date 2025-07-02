package com.madhu.qou.service.rewriting;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import com.madhu.qou.dto.Entity;
import com.madhu.qou.dto.UnderstoodQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QueryRewriteService {

    private final String indexName = "products_index";
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "the", "in", "on", "for", "from",
            "i", "me", "show", "find", "get", "some",
            "items", "products", "aisle"
    );

    public SearchRequest buildEsQuery(UnderstoodQuery understoodQuery) {
        final String originalQuery = understoodQuery.preprocessedQuery().normalizedQuery();
        final List<Entity> entities = understoodQuery.entities();

        List<Query> filterClauses = new ArrayList<>();
        List<Query> mustClauses = new ArrayList<>();

        // --- NEW: Entity-First Logic ---
        Set<String> entityTexts = new HashSet<>();

        // 1. Create high-priority filters from specific entities
        for (var entity : entities) {
            entityTexts.add(entity.value());
            switch (entity.type()) {
                case "BRAND" ->
                    filterClauses.add(
                        TermQuery.of(t -> t
                            .field("brand")
                            .value(entity.value())
                            .caseInsensitive(true)
                        )._toQuery()
                    );
                case "DIETARY_ATTRIBUTE", "GROCERY_ATTRIBUTE" -> {
                    if ("organic".equalsIgnoreCase(entity.value())) {
                        filterClauses.add(TermQuery.of(t -> t.field("grocery_attributes.is_organic").value(true))._toQuery());
                    }
                }
                case "AISLE" ->
                    filterClauses.add(TermQuery.of(t -> t.field("categories").value(entity.value()))._toQuery());
                case "PRODUCT_TYPE" ->
                    mustClauses.add(MatchQuery.of(m -> m.field("name").query(entity.value()))._toQuery());
            }
        }

        // 2. Determine the residual query text after removing entities and stop words
        String residualQuery = Arrays.stream(originalQuery.split("\\s+"))
                .filter(word -> !entityTexts.contains(word) && !STOP_WORDS.contains(word))
                .collect(Collectors.joining(" "));

        // 3. Add the cleaned residual text as a general search clause, if it's not empty
        if (!mustClauses.isEmpty() && !residualQuery.isBlank()) {
            mustClauses.add(MultiMatchQuery.of(m -> m
                    .query(residualQuery)
                    .fields("name", "description", "search_aid^0.5")
                    .type(TextQueryType.BestFields)
            )._toQuery());
        }

        // If there are no specific must clauses, use the original query as a fallback
        if (mustClauses.isEmpty()) {
            mustClauses.add(MultiMatchQuery.of(m -> m.query(originalQuery).fields("name", "description"))._toQuery());
        }
        // --- End of New Logic ---


        BoolQuery boolQuery = BoolQuery.of(b -> b
                .must(mustClauses)
                .filter(filterClauses)
        );

        log.info("Constructed ES Query DSL: {}", boolQuery.toString());

        Aggregation categoryAgg = Aggregation.of(a -> a.terms(t -> t.field("categories").size(10)));
        Aggregation brandAgg = Aggregation.of(a -> a.terms(t -> t.field("brand").size(10)));
        Suggester didYouMeanSuggester = Suggester.of(sug -> sug.text(originalQuery).suggesters("spell-check", fs -> fs.term(t -> t.field("name"))));

        return SearchRequest.of(s -> s
                .index(indexName)
                .query(q -> q.bool(boolQuery))
                .aggregations("by_category", categoryAgg)
                .aggregations("by_brand", brandAgg)
                .suggest(didYouMeanSuggester)
        );
    }
}