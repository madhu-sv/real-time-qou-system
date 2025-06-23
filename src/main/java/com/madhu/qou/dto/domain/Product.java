package com.madhu.qou.dto.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// This record represents a single product document in Elasticsearch
public record Product(
        @JsonProperty("product_id") String productId,
        String name,
        String description,
        String brand,
        List<String> categories,
        List<Attribute> attributes,
        @JsonProperty("grocery_attributes") GroceryAttributes groceryAttributes,
        @JsonProperty("search_aid") String searchAid // Catch-all field for searching
) {
    // Nested record for the "attributes" field
    public record Attribute(String name, String value) {}

    // Nested record for the "grocery_attributes" object
    public record GroceryAttributes(
            List<String> dietary,
            @JsonProperty("is_organic") boolean isOrganic,
            @JsonProperty("storage_type") String storageType
    ) {}
}
