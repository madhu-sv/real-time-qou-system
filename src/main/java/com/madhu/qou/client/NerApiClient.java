package com.madhu.qou.client;

import com.madhu.qou.dto.ner.NerResponse;
import org.springframework.beans.factory.annotation.Value; // <-- Import
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Component
public class NerApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    // Inject the URL from application properties
    @Value("${ner.api.url}")
    private String nerApiUrl;

    public NerResponse extractEntities(String text) {
        try {
            // The record NerRequest is not defined. We can simply pass the map.
            Map<String, String> requestPayload = Collections.singletonMap("text", text);
            return restTemplate.postForObject(nerApiUrl, requestPayload, NerResponse.class);
        } catch (Exception e) {
            System.err.println("ERROR calling NER API: " + e.getMessage());
            return new NerResponse(Collections.emptyList());
        }
    }
}