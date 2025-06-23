package com.madhu.qou.dto;

public record PreprocessedQuery(
        String originalQuery,
        String normalizedQuery,
        UserContext userContext
) {}