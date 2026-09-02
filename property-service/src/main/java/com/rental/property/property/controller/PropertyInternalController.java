package com.rental.property.property.controller;

import com.rental.property.property.entity.Property;
import com.rental.property.property.repo.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal, service-to-service API consumed by tenant-service to browse/search
 * listings. This replaces tenant-service's direct PropertyRepository access from
 * the original monolith, since Property now lives only in this service's database.
 * Not exposed through the API gateway.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/properties")
public class
PropertyInternalController {

    private final PropertyRepository propertyRepository;

    @GetMapping
    public List<Property> getAllProperties(){
        log.info("[internal] Fetching all properties");
        return propertyRepository.findAll();
    }

    @GetMapping("/search")
    public List<Property> searchProperties(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false) String bhk){
        log.info("[internal] Searching properties with city: {}, minPrice: {}, maxPrice: {}, bhk: {}", city, minPrice, maxPrice, bhk);
        return propertyRepository.searchProperties(city, maxPrice, minPrice, bhk);
    }
}
