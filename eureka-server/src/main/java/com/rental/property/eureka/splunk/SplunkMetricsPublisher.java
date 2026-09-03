package com.rental.property.eureka.splunk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Map;

/**
 * Pushes JVM metrics into Splunk's "renthub_metrics" index every 15s, via HEC's metrics
 * event format. This is the data the "mstats" panels in the RentHub Service Health
 * dashboard read from - see docs/SPLUNK_GUIDE.md.
 */
@Component
public class SplunkMetricsPublisher {

    private final SplunkHecClient splunkHecClient;
    private final String serviceName;
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public SplunkMetricsPublisher(SplunkHecClient splunkHecClient,
                                   @Value("${spring.application.name}") String serviceName) {
        this.splunkHecClient = splunkHecClient;
        this.serviceName = serviceName;
    }

    @Scheduled(fixedRate = 15000)
    public void publish() {
        splunkHecClient.sendMetric("renthub_metrics", Map.of(
                "metric_name:jvm.memory.heap.used", memoryMXBean.getHeapMemoryUsage().getUsed(),
                "metric_name:jvm.thread.count", threadMXBean.getThreadCount(),
                "service", serviceName
        ));
    }
}
