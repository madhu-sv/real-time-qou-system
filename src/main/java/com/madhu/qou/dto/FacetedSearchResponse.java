package com.madhu.qou.dto;

import com.madhu.qou.dto.domain.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

// The main response object
public record FacetedSearchResponse(
        List<Product> products,
        List<Facet> facets,
        // Add a field for our suggestion. It will only appear in the JSON if it's not null.
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String didYouMeanSuggestion
) {}
