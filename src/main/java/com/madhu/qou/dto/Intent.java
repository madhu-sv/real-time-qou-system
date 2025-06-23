package com.madhu.qou.dto;

public record Intent(String name, double confidence) {
    // Overloaded constructor for default confidence
    public Intent(String name) {
        this(name, 1.0);
    }
}
