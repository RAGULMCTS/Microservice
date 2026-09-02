package com.rental.property.tenant.client;

import com.rental.property.tenant.model.Property;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "property-service")
public interface PropertyServiceClient {

    @GetMapping("/api/internal/properties")
    List<Property> getAllProperties();

    @GetMapping("/api/internal/properties/search")
    List<Property> searchProperties(
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "minPrice", required = false) String minPrice,
            @RequestParam(value = "maxPrice", required = false) String maxPrice,
            @RequestParam(value = "bhk", required = false) String bhk);
}
