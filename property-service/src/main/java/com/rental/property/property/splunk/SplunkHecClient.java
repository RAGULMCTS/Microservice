package com.rental.property.property.splunk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Minimal client for Splunk's HTTP Event Collector (HEC). Two payload shapes are used:
 * business "events" (index=renthub_events) and JVM/request "metrics" (index=renthub_metrics,
 * HEC's metric event format - see https://docs.splunk.com/Documentation/Splunk/latest/Metrics/GetMetricsInHEC).
 * Sends are fire-and-forget: a Splunk hiccup must never break the calling request.
 */
@Component
public class SplunkHecClient {

    private static final Logger log = LoggerFactory.getLogger(SplunkHecClient.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String hecUrl;
    private final String token;
    private final boolean enabled;

    public SplunkHecClient(@Value("${splunk.hec.url:http://localhost:8088}") String hecUrl,
                            @Value("${splunk.hec.token:}") String hecToken) {
        this.hecUrl = hecUrl;
        this.token = hecToken;
        this.enabled = hecToken != null && !hecToken.isBlank();
    }

    public void sendEvent(String sourcetype, String index, Map<String, Object> event) {
        if (!enabled) {
            return;
        }
        post(Map.of("sourcetype", sourcetype, "index", index, "event", event));
    }

    public void sendMetric(String index, Map<String, Object> fields) {
        if (!enabled) {
            return;
        }
        post(Map.of("sourcetype", "renthub:metrics", "index", index, "event", "metric", "fields", fields));
    }

    private void post(Map<String, Object> payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(hecUrl + "/services/collector/event"))
                    .timeout(Duration.ofSeconds(2))
                    .header("Authorization", "Splunk " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(error -> {
                        log.debug("HEC send failed: {}", error.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.debug("HEC send failed: {}", e.getMessage());
        }
    }
}
