package com.rental.property.gateway.splunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Minimal client for Splunk's HTTP Event Collector (HEC). Two payload shapes are used:
 * business "events" (index=renthub_events) and JVM/request "metrics" (index=renthub_metrics,
 * HEC's metric event format - see https://docs.splunk.com/Documentation/Splunk/latest/Metrics/GetMetricsInHEC).
 * Sends are fire-and-forget: a Splunk hiccup must never fail a gateway request.
 */
@Component
public class SplunkHecClient {

    private static final Logger log = LoggerFactory.getLogger(SplunkHecClient.class);

    private final WebClient webClient;
    private final String token;
    private final boolean enabled;

    public SplunkHecClient(WebClient.Builder webClientBuilder,
                            @Value("${splunk.hec.url:http://localhost:8088}") String hecUrl,
                            @Value("${splunk.hec.token:}") String hecToken) {
        this.token = hecToken;
        this.enabled = hecToken != null && !hecToken.isBlank();
        this.webClient = webClientBuilder.baseUrl(hecUrl).build();
    }

    public void sendEvent(String sourcetype, String index, Map<String, Object> event) {
        if (!enabled) {
            return;
        }
        post(Map.of(
                "sourcetype", sourcetype,
                "index", index,
                "event", event
        ));
    }

    public void sendMetric(String index, Map<String, Object> fields) {
        if (!enabled) {
            return;
        }
        post(Map.of(
                "sourcetype", "renthub:metrics",
                "index", index,
                "event", "metric",
                "fields", fields
        ));
    }

    private void post(Map<String, Object> payload) {
        webClient.post()
                .uri("/services/collector/event")
                .header("Authorization", "Splunk " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> { },
                        error -> log.debug("HEC send failed: {}", error.getMessage())
                );
    }
}
