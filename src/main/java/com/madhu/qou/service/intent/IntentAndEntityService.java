package com.madhu.qou.service.intent;

import com.madhu.qou.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class IntentAndEntityService {

    private static final Set<String> BRANDS = Set.of("nike", "samsung", "sony");
    private static final Set<String> COLORS = Set.of("red", "blue", "black");
    private static final Set<String> GROCERY_KEYWORDS = Set.of("organic", "gluten-free");

    public UnderstoodQuery process(PreprocessedQuery preprocessedQuery) {
        final String query = preprocessedQuery.normalizedQuery();
        final List<Entity> entities = new ArrayList<>();

        log.info("--- Intent & Entity Step ---");
        log.info("Processing normalized query: '{}'", query);

        BRANDS.forEach(brand -> {
            if (query.contains(brand)) {
                entities.add(new Entity(brand, "BRAND", query.indexOf(brand), query.indexOf(brand) + brand.length()));
            }
        });
        COLORS.forEach(color -> {
            if (query.contains(color)) {
                entities.add(new Entity(color, "ATTRIBUTE_COLOR", query.indexOf(color), query.indexOf(color) + color.length()));
            }
        });
        GROCERY_KEYWORDS.forEach(keyword -> {
            if (query.contains(keyword)) {
                entities.add(new Entity(keyword, "GROCERY_ATTRIBUTE", query.indexOf(keyword), query.indexOf(keyword) + keyword.length()));
            }
        });

        log.info("Found {} entities: {}", entities.size(), entities);

        final Intent intent = (query.startsWith("how to") || query.startsWith("what is")) ?
                new Intent("informational") :
                new Intent("find_product");

        log.info("Detected intent: '{}'", intent.name());

        return new UnderstoodQuery(preprocessedQuery, intent, entities);
    }
}