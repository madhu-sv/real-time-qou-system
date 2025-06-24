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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final QueryUnderstandingService queryUnderstandingService;

    @PostMapping("/search")
    public FacetedSearchResponse search(@RequestBody CustomSearchRequest request) {
        log.info("================== NEW SEARCH REQUEST ==================");
        log.info("Received rawQuery: '{}'", request.rawQuery());
        return queryUnderstandingService.processFacetedQuery(request);
    }

    @GetMapping("/suggest")
    public List<String> getSuggestions(@RequestParam String prefix) {
        return queryUnderstandingService.getSuggestions(prefix);
    }

    @PostMapping("/understand-query")
    public Map<String, Object> understandQuery(@RequestBody CustomSearchRequest request) {
        log.info("================== DEBUG UNDERSTAND REQUEST ==================");
        log.info("Received rawQuery: '{}'", request.rawQuery());
        var esQuery = queryUnderstandingService.getDebugQuery(request);

        return Map.of(
                "index", esQuery.index().get(0),
                "query", esQuery.query(),
                "aggregations", esQuery.aggregations()
        );
    }
}