package com.madhu.qou.controller;

import com.madhu.qou.dto.CustomSearchRequest;
import com.madhu.qou.dto.FacetedSearchResponse;
import com.madhu.qou.service.QueryUnderstandingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1") // Moving "/search" down to the method level for clarity
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final QueryUnderstandingService queryUnderstandingService;

    // The main, public-facing search endpoint
    @PostMapping("/search")
    public FacetedSearchResponse search(@RequestBody CustomSearchRequest request) {
        log.info("================== NEW SEARCH REQUEST ==================");
        log.info("Received rawQuery: '{}'", request.rawQuery());
        return queryUnderstandingService.processFacetedQuery(request);
    }

    // The autocomplete endpoint
    @GetMapping("/suggest")
    public List<String> getSuggestions(@RequestParam String prefix) {
        return queryUnderstandingService.getSuggestions(prefix);
    }

    // The developer/debugging endpoint
    @PostMapping("/understand-query")
    public Map<String, Object> understandQuery(@RequestBody CustomSearchRequest request) {
        log.info("================== DEBUG UNDERSTAND REQUEST ==================");
        log.info("Received rawQuery: '{}'", request.rawQuery());
        var esQuery = queryUnderstandingService.getDebugQuery(request);

        // As we discovered, .toString() is more reliable for logging,
        // but for the API response, this structure is fine for a debug tool.
        return Map.of(
                "index", esQuery.index().get(0),
                "query", esQuery.query(),
                "aggregations", esQuery.aggregations()
        );
    }
}