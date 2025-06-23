package com.madhu.qou.controller;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.madhu.qou.dto.CustomSearchRequest;
import com.madhu.qou.service.QueryUnderstandingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final QueryUnderstandingService queryUnderstandingService;

    @PostMapping("/understand")
    public Map<String, Object> understandQuery(@RequestBody CustomSearchRequest request) {
        // Log the incoming request right at the entry point
        log.info("================== NEW REQUEST ==================");
        log.info("Received rawQuery: '{}'", request.rawQuery());

        SearchRequest esQuery = queryUnderstandingService.processQuery(request);

        return Map.of(
                "index", esQuery.index().get(0),
                "query", esQuery.query()
        );
    }

    // Add this new method to your SearchController
    @GetMapping("/suggest")
    public List<String> getSuggestions(@RequestParam String prefix) {
        return queryUnderstandingService.getSuggestions(prefix);
    }
}