package com.madhu.qou.dto;

public record Entity(
        String value,
        String type,
        int startPosition,
        int endPosition
) {}
