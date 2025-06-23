package com.madhu.qou.service.ingestion;

import com.madhu.qou.dto.PreprocessedQuery;
import com.madhu.qou.dto.CustomSearchRequest;
import org.springframework.stereotype.Service;

import java.text.Normalizer;

@Service
public class PreprocessingService {

    public PreprocessedQuery process(CustomSearchRequest request) {
        String normalized = normalizeQuery(request.rawQuery());
        return new PreprocessedQuery(
                request.rawQuery(),
                normalized,
                request.userContext()
        );
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        // 1. Trim and Lowercase
        String processedQuery = query.trim().toLowerCase();
        // 2. Fold Diacritics
        processedQuery = Normalizer.normalize(processedQuery, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        // 3. Remove non-essential special characters
        processedQuery = processedQuery.replaceAll("[^a-z0-9\\s-]", "");
        return processedQuery;
    }
}
