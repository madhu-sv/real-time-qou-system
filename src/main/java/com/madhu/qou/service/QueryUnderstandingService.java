package com.madhu.qou.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.madhu.qou.dto.CustomSearchRequest;
import com.madhu.qou.dto.PreprocessedQuery;
import com.madhu.qou.dto.UnderstoodQuery;
import com.madhu.qou.service.ingestion.PreprocessingService;
import com.madhu.qou.service.intent.IntentAndEntityService;
import com.madhu.qou.service.rewriting.QueryRewriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryUnderstandingService {

    private final ElasticsearchClient esClient; // <-- Make sure this is injected
    private final PreprocessingService preprocessingService;
    private final IntentAndEntityService intentAndEntityService;
    private final QueryRewriteService queryRewriteService;

    public SearchRequest processQuery(CustomSearchRequest request) {
        // 1. Ingestion & Pre-processing
        PreprocessedQuery preprocessedQuery = preprocessingService.process(request);

        // 2. Intent & Entity Recognition
        UnderstoodQuery understoodQuery = intentAndEntityService.process(preprocessedQuery);

        // 3. Enrichment & Rewriting
        log.info("--- Rewriting Step ---");
        log.info("Building final query with {} entities.", understoodQuery.entities().size());
        return queryRewriteService.buildEsQuery(understoodQuery);
    }

    // This is the method we are fixing
    public List<String> getSuggestions(String prefix) {
        final String suggestionIndexName = "suggestions_index";
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(suggestionIndexName)
                    .suggest(sug -> sug
                            .text(prefix) // <-- FIX: The prefix is set here using .text()
                            .suggesters("product-suggester", fs -> fs
                                    .completion(cs -> cs
                                            .field("suggest")
                                            // .prefix(prefix) <-- This was incorrect and has been removed
                                            .size(10)
                                            .skipDuplicates(true)
                                    )
                            )
                    )
            );

            SearchResponse<Void> response = esClient.search(searchRequest, Void.class);

            // Extract the text from the suggestion options
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