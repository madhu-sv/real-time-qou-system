package com.madhu.qou.service.intent;

import com.madhu.qou.client.NerApiClient;
import com.madhu.qou.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class IntentAndEntityService {

    // Inject our new API client
    private final NerApiClient nerApiClient;

    // Fallback brand list (same set as DataSeeder KNOWN_BRANDS)
    private static final Set<String> BRANDS = Set.of(
            "Fage", "Annie's", "Newman's Own", "General Mills", "Blue Diamond",
            "Bonne Maman", "Philadelphia", "Stacy's", "Kellogg's", "Horizon Organic"
    );

    public UnderstoodQuery process(PreprocessedQuery preprocessedQuery) {
        final String query = preprocessedQuery.normalizedQuery();
        log.info("--- ML-based Intent & Entity Step ---");
        log.info("Sending to NER API: '{}'", query);

        // Call the NER API to get entities
        var nerResponse = nerApiClient.extractEntities(query);

        // Transform the NER API's response into our application's internal Entity DTO
        List<Entity> entities = nerResponse.entities().stream()
                .map(nerEntity -> new Entity(nerEntity.text(), nerEntity.label(), -1, -1))
                .collect(Collectors.toList());

        // Fallback: if NER didn't extract a BRAND, check for known brand keywords
        if (entities.stream().noneMatch(e -> "BRAND".equals(e.type()))) {
            for (String brand : BRANDS) {
                String lower = brand.toLowerCase();
                if (query.contains(lower)) {
                    entities.add(new Entity(lower, "BRAND", query.indexOf(lower), query.indexOf(lower) + lower.length()));
                }
            }
        }

        log.info("Found {} entities using NER model: {}", entities.size(), entities);

        // Keep intent detection simple for now
        final Intent intent = new Intent("find_product");
        log.info("Detected intent: '{}'", intent.name());

        return new UnderstoodQuery(preprocessedQuery, intent, entities);
    }
}