package com.madhu.qou.dto.ner;

import com.fasterxml.jackson.annotation.JsonProperty;

// This record matches each object in the "entities" list
public record NerEntity(String text, @JsonProperty("label") String label) {}