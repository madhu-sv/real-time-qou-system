package com.madhu.qou.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.elasticsearch.core.search.TermSuggest; // <-- Import this
import com.madhu.qou.dto.*;
import com.madhu.qou.dto.domain.Product;
import com.madhu.qou.service.ingestion.PreprocessingService;
import com.madhu.qou.service.intent.IntentAndEntityService;
import com.madhu.qou.service.rewriting.QueryRewriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
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
        // Build the request, which also performs NER
        UnderstoodQuery understoodQuery = understandQuery(request);
        SearchRequest esRequest = queryRewriteService.buildEsQuery(understoodQuery);

        try {
            SearchResponse<Product> esResponse = esClient.search(esRequest, Product.class);

            List<Product> products = esResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            List<Facet> facets = parseFacets(esResponse.aggregations());
            String suggestion = null;

            assert esResponse.hits().total() != null;
            if (esResponse.hits().total().value() == 0 && esResponse.suggest() != null) {
                List<Suggestion<Product>> spellCheckSuggestions = esResponse.suggest().get("spell-check");
                if (spellCheckSuggestions != null && !spellCheckSuggestions.isEmpty()) {
                    // Pass the list of found entities to the suggestion builder
                    suggestion = buildSuggestionString(spellCheckSuggestions, understoodQuery.entities());
                }
            }

            return new FacetedSearchResponse(products, facets, suggestion);

        } catch (IOException e) {
            log.error("Error during faceted search execution", e);
            return new FacetedSearchResponse(Collections.emptyList(), Collections.emptyList(), null);
        }
    }

    // Renamed this method for clarity and to be re-used
    private UnderstoodQuery understandQuery(CustomSearchRequest request) {
        PreprocessedQuery preprocessedQuery = preprocessingService.process(request);
        return intentAndEntityService.process(preprocessedQuery);
    }

    public SearchRequest getDebugQuery(CustomSearchRequest request) {
        return buildSearchRequest(request);
    }

    private SearchRequest buildSearchRequest(CustomSearchRequest request) {
        PreprocessedQuery preprocessedQuery = preprocessingService.process(request);
        UnderstoodQuery understoodQuery = intentAndEntityService.process(preprocessedQuery);
        return queryRewriteService.buildEsQuery(understoodQuery);
    }

    private String buildSuggestionString(List<Suggestion<Product>> suggestions, List<Entity> entities) {
        // Create a set of all the words that were identified as entities for quick lookup
        Set<String> entityWords = entities.stream()
                .flatMap(entity -> Arrays.stream(entity.value().split("\\s+")))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        StringBuilder correctedQuery = new StringBuilder();
        boolean wasCorrectionMade = false;

        for (var suggestion : suggestions) {
            TermSuggest term = suggestion.term();
            String originalWord = term.text();

            // Only use the suggestion if the original word is NOT a known entity
            if (!entityWords.contains(originalWord.toLowerCase()) && !term.options().isEmpty()) {
                correctedQuery.append(term.options().get(0).text());
                wasCorrectionMade = true; // Mark that we made at least one change
            } else {
                // Otherwise, keep the original word
                correctedQuery.append(originalWord);
            }
            correctedQuery.append(" ");
        }

        // Only return a suggestion if we actually corrected something
        return wasCorrectionMade ? correctedQuery.toString().trim() : null;
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
                            .text(prefix)
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