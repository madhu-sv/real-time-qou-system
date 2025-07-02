package com.madhu.qou.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import com.madhu.qou.dto.domain.Product;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
// NOTE: We have removed "implements CommandLineRunner"
public class DataSeeder {

    private final ElasticsearchClient esClient;
    private static final Set<String> KNOWN_BRANDS = Set.of(
            "Fage", "Annie's", "Newman's Own", "General Mills", "Blue Diamond",
            "Bonne Maman", "Philadelphia", "Stacy's", "Kellogg's", "Horizon Organic",
            "Nike"
    );

    // This method is now public
    public void seedProductIndex() throws IOException, CsvValidationException {
        final String indexName = "products_index";
        if (esClient.indices().exists(req -> req.index(indexName)).value()) {
            log.info("Product index '{}' already exists. Skipping seeding.", indexName);
            return;
        }

        log.info("Creating product index '{}'", indexName);
        esClient.indices().create(c -> c
                .index(indexName)
                .mappings(m -> m
                        .properties("attributes", p -> p.nested(n -> n))
                        .properties("brand", p -> p.keyword(k -> k))
                        .properties("categories", p -> p.keyword(k -> k))
                )
        );

        Map<String, String> aisleMap = loadAisles();
        List<Product> products = new ArrayList<>();
        ClassPathResource productsResource = new ClassPathResource("data/products.csv");

        try (CSVReader reader = new CSVReader(new InputStreamReader(productsResource.getInputStream()))) {
            reader.readNext(); // Skip header
            String[] line;
            while ((line = reader.readNext()) != null) {
                String category = aisleMap.getOrDefault(line[2], "Unknown");
                String productName = line[1];
                String brand = extractBrand(productName);
                boolean isOrganic = productName.toLowerCase().contains("organic")
                        || brand.toLowerCase().contains("organic");
                Product.GroceryAttributes groceryAttributes = new Product.GroceryAttributes(
                        List.of(), isOrganic, null
                );
                String searchAid = productName.toLowerCase() + " " + category.toLowerCase();
                products.add(new Product(
                        "instacart-" + line[0],
                        productName,
                        "",
                        brand,
                        List.of(category),
                        List.of(),
                        groceryAttributes,
                        searchAid
                ));
                if (products.size() >= 1000) {
                    indexProductsInBulk(products);
                    products.clear();
                }
            }
        }
        if (!products.isEmpty()) {
            indexProductsInBulk(products);
        }
        log.info("Finished seeding product index.");
    }

    // This method is also now public
    public void seedSuggestionIndex() throws IOException, CsvValidationException {
        final String indexName = "suggestions_index";
        if (esClient.indices().exists(req -> req.index(indexName)).value()) {
            log.info("Suggestion index '{}' already exists. Skipping seeding.", indexName);
            return;
        }

        log.info("Creating suggestion index '{}'", indexName);
        esClient.indices().create(c -> c.index(indexName).mappings(m -> m.properties("suggest", p -> p.completion(comp -> comp))));

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
        BulkRequest finalRequest = br.build();
        if (!finalRequest.operations().isEmpty()) {
            esClient.bulk(finalRequest);
        }
        log.info("Finished seeding suggestion index with {} entries.", count);
    }

    private Map<String, String> loadAisles() throws IOException, CsvValidationException {
        Map<String, String> aisleMap = new HashMap<>();
        ClassPathResource aislesResource = new ClassPathResource("data/aisles.csv");
        try (CSVReader reader = new CSVReader(new InputStreamReader(aislesResource.getInputStream()))) {
            reader.readNext();
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

    private String extractBrand(String productName) {
        String productNameLower = productName.toLowerCase();
        for (String brand : KNOWN_BRANDS) {
            if (productNameLower.contains(brand.toLowerCase())) {
                return brand;
            }
        }
        return "Private Label";
    }
}