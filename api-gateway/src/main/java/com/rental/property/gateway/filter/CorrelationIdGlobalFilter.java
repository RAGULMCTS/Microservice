package com.rental.property.gateway.filter;

import com.rental.property.gateway.splunk.SplunkHecClient;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates (or trusts an inbound) X-Request-Id, propagates it to downstream services,
 * and emits one HEC "gateway_request_completed" event per request. This is the thread
 * that later lets you run a single Splunk search that joins renthub_logs and
 * renthub_events across every service by requestId (see docs/SPLUNK_GUIDE.md).
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdGlobalFilter.class);
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final SplunkHecClient splunkHecClient;
    private final AtomicLong requestCount = new AtomicLong();
    private final AtomicLong errorCount = new AtomicLong();

    public CorrelationIdGlobalFilter(SplunkHecClient splunkHecClient) {
        this.splunkHecClient = splunkHecClient;
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest originalRequest = exchange.getRequest();
        String requestId = originalRequest.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        final String finalRequestId = requestId;

        ServerHttpRequest mutatedRequest = originalRequest.mutate()
                .header(REQUEST_ID_HEADER, finalRequestId)
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(REQUEST_ID_HEADER, finalRequestId);

        Instant start = Instant.now();
        log.info("gateway_request_started path={} method={} reqId={}",
                originalRequest.getPath(), originalRequest.getMethod(),
                StructuredArguments.value("requestId", finalRequestId));

        return chain.filter(mutatedExchange)
                .doOnSuccess(v -> recordCompletion(originalRequest, response, finalRequestId, start))
                .doOnError(err -> recordCompletion(originalRequest, response, finalRequestId, start));
    }

    private void recordCompletion(ServerHttpRequest request, ServerHttpResponse response,
                                    String requestId, Instant start) {
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        int status = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
        requestCount.incrementAndGet();
        if (status >= 500) {
            errorCount.incrementAndGet();
        }

        log.info("gateway_request_completed path={} method={} status={} durationMs={} reqId={}",
                request.getPath(), request.getMethod(), status, durationMs,
                StructuredArguments.value("requestId", requestId));

        splunkHecClient.sendEvent("renthub:appevent", "renthub_events", Map.of(
                "type", "gateway_request_completed",
                "service", "api-gateway",
                "path", request.getPath().value(),
                "method", request.getMethod() != null ? request.getMethod().name() : "UNKNOWN",
                "status", status,
                "durationMs", durationMs,
                "requestId", requestId
        ));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
