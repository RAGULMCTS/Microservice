package com.rental.property.tenant.service;

import com.rental.property.tenant.client.PropertyServiceClient;
import com.rental.property.tenant.model.Property;
import com.rental.property.tenant.splunk.SplunkHecClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class TenantServiceImpl  implements TenantService{

    @Autowired
    private PropertyServiceClient propertyServiceClient;

    @Autowired
    private SplunkHecClient splunkHecClient;

    @Override
    public List<Property> getAllProperty() {
        return propertyServiceClient.getAllProperties();
    }

    @Override
    public List<Property> searchProperties(String city, String minPrice, String maxPrice, String bhk) {
        List<Property> results = propertyServiceClient.searchProperties(city,minPrice,maxPrice,bhk);
        splunkHecClient.sendEvent("renthub:appevent", "renthub_events", Map.of(
                "type", "tenant_property_search",
                "service", "tenant-service",
                "city", String.valueOf(city),
                "bhk", String.valueOf(bhk),
                "resultCount", results.size(),
                "requestId", String.valueOf(MDC.get("requestId"))
        ));
        return results;
    }


}
