package com.madhu.qou.dto;

import com.madhu.qou.dto.domain.Product;
import java.util.List;

// The main response object
public record FacetedSearchResponse(
        List<Product> products,
        List<Facet> facets
) {}