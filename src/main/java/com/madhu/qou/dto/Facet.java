package com.madhu.qou.dto;

import java.util.List;

// Represents a single facet, e.g., "Brand"
public record Facet(
        String name,
        List<FacetValue> values
) {}