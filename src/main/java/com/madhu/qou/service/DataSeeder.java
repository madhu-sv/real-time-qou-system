package com.madhu.qou.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.madhu.qou.dto.domain.Product;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ElasticsearchClient esClient;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting data seeding process...");
        seedProductIndex();
        seedSuggestionIndex();
        log.info("Data seeding process complete.");
    }

    private void seedProductIndex() throws IOException, CsvValidationException {
        final String indexName = "products_index";

        if (esClient.indices().exists(req -> req.index(indexName)).value()) {
            log.info("Product index '{}' already exists. Skipping.", indexName);
            return;
        }

        log.info("Creating product index '{}'", indexName);
        esClient.indices().create(c -> c
                .index(indexName)
                .mappings(m -> m
                        .properties("attributes", p -> p.nested(n -> n))
                        .properties("brand", p -> p.keyword(k -> k))
                )
        );

        Map<String, String> aisleMap = loadAisles();
        List<Product> products = new ArrayList<>();
        ClassPathResource productsResource = new ClassPathResource("data/products.csv");

        try (CSVReader reader = new CSVReader(new InputStreamReader(productsResource.getInputStream()))) {
            reader.readNext(); // Skip header
            String[] line;
            while ((line = reader.readNext()) != null) {
                String aisleId = line[2];
                String category = aisleMap.getOrDefault(aisleId, "Unknown");
                String productName = line[1];

                products.add(new Product(
                        "instacart-" + line[0], productName, "", "instacart",
                        List.of(category), null, null,
                        productName.toLowerCase() + " " + category.toLowerCase()
                ));

                if (products.size() >= 10000) {
                    indexProductsInBulk(products);
                    products.clear();
                }
            }
        }
        // FIX: Build the request only once for the remaining products
        if (!products.isEmpty()) {
            indexProductsInBulk(products);
        }
        log.info("Finished seeding product index.");
    }

    private void seedSuggestionIndex() throws IOException, CsvValidationException {
        final String indexName = "suggestions_index";

        if (esClient.indices().exists(req -> req.index(indexName)).value()) {
            log.info("Suggestion index '{}' already exists. Skipping.", indexName);
            return;
        }

        log.info("Creating suggestion index '{}'", indexName);
        esClient.indices().create(c -> c
                .index(indexName)
                .mappings(m -> m
                        .properties("suggest", p -> p.completion(comp -> comp))
                )
        );

        ClassPathResource productsResource = new ClassPathResource("data/products.csv");
        BulkRequest.Builder br = new BulkRequest.Builder();
        int count = 0;

        try (CSVReader reader = new CSVReader(new InputStreamReader(productsResource.getInputStream()))) {
            reader.readNext(); // Skip header
            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, Object> doc = new HashMap<>();
                doc.put("product_id", "instacart-" + line[0]);
                doc.put("product_name", line[1]);
                doc.put("suggest", Map.of("input", line[1]));

                br.operations(op -> op.index(idx -> idx.index(indexName).document(doc)));
                count++;
                if (count % 10000 == 0) {
                    esClient.bulk(br.build());
                    br = new BulkRequest.Builder();
                    log.info("Indexed {} suggestions...", count);
                }
            }
        }

        // FIX: Build the request only once for the final batch
        BulkRequest finalRequest = br.build();
        if (!finalRequest.operations().isEmpty()) {
            esClient.bulk(finalRequest);
        }
        log.info("Finished seeding suggestion index with {} entries.", count);
    }

    // --- Helper methods below ---

    private Map<String, String> loadAisles() throws IOException, CsvValidationException {
        Map<String, String> aisleMap = new HashMap<>();
        ClassPathResource aislesResource = new ClassPathResource("data/aisles.csv");
        try (CSVReader reader = new CSVReader(new InputStreamReader(aislesResource.getInputStream()))) {
            reader.readNext(); // Skip header
            String[] line;
            while ((line = reader.readNext()) != null) {
                aisleMap.put(line[0], line[1]);
            }
        }
        return aisleMap;
    }

    private void indexProductsInBulk(List<Product> products) throws IOException {
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (Product product : products) {
            br.operations(op -> op.index(idx -> idx.index("products_index").id(product.productId()).document(product)));
        }
        esClient.bulk(br.build());
        log.info("Indexed a batch of {} products.", products.size());
    }
}