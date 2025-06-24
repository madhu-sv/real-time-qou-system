package com.madhu.qou.service.rewriting;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
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

    private final String indexName = "products_index";
    private static final Map<String, String> SYNONYM_MAP = Map.of("shoes", "footwear");

    public SearchRequest buildEsQuery(UnderstoodQuery understoodQuery) {
        final String queryText = understoodQuery.preprocessedQuery().normalizedQuery();
        final var entities = understoodQuery.entities();

        Query multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(queryText)
                .fields("name", "description", "brand", "search_aid^0.5")
                .type(TextQueryType.BestFields)
        )._toQuery();

        List<Query> filterClauses = new ArrayList<>();
        for (var entity : entities) {
            switch (entity.type()) {
            case "BRAND" ->
                        filterClauses.add(TermQuery.of(t -> t
                                .field("brand")
                                .value(entity.value())
                                .caseInsensitive(true)
                        )._toQuery());
                case "GROCERY_ATTRIBUTE" -> {
                    if ("organic".equals(entity.value())) {
                        filterClauses.add(TermQuery.of(t -> t.field("grocery_attributes.is_organic").value(true))._toQuery());
                    }
                }
            }
        }

        BoolQuery boolQuery = BoolQuery.of(b -> {
            // If a BRAND entity was detected, only apply the filter clause
            if (entities.stream().anyMatch(e -> "BRAND".equals(e.type()))) {
                b.filter(filterClauses);
            } else {
                b.must(multiMatchQuery).filter(filterClauses);
            }
            return b;
        });

        log.info("Constructed ES Query DSL: {}", boolQuery.toString());

        Aggregation categoryAgg = Aggregation.of(a -> a
                .terms(t -> t.field("categories").size(10))
        );
        Aggregation brandAgg = Aggregation.of(a -> a
                .terms(t -> t.field("brand").size(10))
        );

        return SearchRequest.of(s -> s
                .index(indexName)
                .query(q -> q.bool(boolQuery))
                .aggregations("by_category", categoryAgg)
                .aggregations("by_brand", brandAgg)
                .suggest(sug -> sug
                        .text(queryText)
                        .suggesters("spell-check", fs -> fs
                                .term(t -> t.field("name"))
                        )
                )
        );
    }
}