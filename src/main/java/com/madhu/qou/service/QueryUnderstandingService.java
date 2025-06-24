package com.madhu.qou.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.madhu.qou.dto.*;
import com.madhu.qou.dto.domain.Product;
import com.madhu.qou.service.ingestion.PreprocessingService;
import com.madhu.qou.service.intent.IntentAndEntityService;
import com.madhu.qou.service.rewriting.QueryRewriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryUnderstandingService {

    private final ElasticsearchClient esClient;
    private final PreprocessingService preprocessingService;
    private final IntentAndEntityService intentAndEntityService;
    private final QueryRewriteService queryRewriteService;

    public FacetedSearchResponse processFacetedQuery(CustomSearchRequest request) {
        SearchRequest esRequest = buildSearchRequest(request);

        try {
            SearchResponse<Product> esResponse = esClient.search(esRequest, Product.class);

            List<Product> products = esResponse.hits().hits().stream()
                    .map(hit -> hit.source())
                    .collect(Collectors.toList());

            List<Facet> facets = parseFacets(esResponse.aggregations());

            return new FacetedSearchResponse(products, facets);

        } catch (IOException e) {
            log.error("Error during faceted search execution", e);
            return new FacetedSearchResponse(Collections.emptyList(), Collections.emptyList());
        }
    }

    public SearchRequest getDebugQuery(CustomSearchRequest request) {
        return buildSearchRequest(request);
    }

    private SearchRequest buildSearchRequest(CustomSearchRequest request) {
        PreprocessedQuery preprocessedQuery = preprocessingService.process(request);
        UnderstoodQuery understoodQuery = intentAndEntityService.process(preprocessedQuery);
        return queryRewriteService.buildEsQuery(understoodQuery);
    }

    private List<Facet> parseFacets(Map<String, Aggregate> aggregations) {
        List<Facet> facets = new ArrayList<>();
        if (aggregations.containsKey("by_category")) {
            List<FacetValue> categoryValues = aggregations.get("by_category").sterms().buckets().array().stream()
                    .map(bucket -> new FacetValue(bucket.key().stringValue(), bucket.docCount()))
                    .collect(Collectors.toList());
            if (!categoryValues.isEmpty()) {
                facets.add(new Facet("Category", categoryValues));
            }
        }
        if (aggregations.containsKey("by_brand")) {
            List<FacetValue> brandValues = aggregations.get("by_brand").sterms().buckets().array().stream()
                    .map(bucket -> new FacetValue(bucket.key().stringValue(), bucket.docCount()))
                    .collect(Collectors.toList());
            if (!brandValues.isEmpty()) {
                facets.add(new Facet("Brand", brandValues));
            }
        }
        return facets;
    }

    public List<String> getSuggestions(String prefix) {
        final String suggestionIndexName = "suggestions_index";
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(suggestionIndexName)
                    .suggest(sug -> sug
                            .suggesters("product-suggester", fs -> fs
                                    .completion(cs -> cs
                                            .field("suggest")
                                            .size(10)
                                            .skipDuplicates(true)
                                    )
                            )
                    )
            );

            SearchResponse<Void> response = esClient.search(searchRequest, Void.class);

            return response.suggest().get("product-suggester").get(0)
                    .completion().options().stream()
                    .map(option -> option.text())
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("Error fetching suggestions", e);
            return Collections.emptyList();
        }
    }
}