package com.madhu.qou.dto;

// Represents a single value within a facet, e.g., "Alpro" with a count of 7
public record FacetValue(
        String value,
        long count
) {}