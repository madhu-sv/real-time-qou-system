package com.madhu.qou.dto;

import java.util.List;

public record UnderstoodQuery(
        PreprocessedQuery preprocessedQuery,
        Intent intent,
        List<Entity> entities
) {}
