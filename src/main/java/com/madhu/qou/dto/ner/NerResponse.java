package com.madhu.qou.dto.ner;

import java.util.List;

// This record matches the top-level JSON response: {"entities": [...]}
public record NerResponse(List<NerEntity> entities) {}