package com.rental.property.tenant.controller;


import com.rental.property.tenant.model.Property;
import com.rental.property.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenant")
public class TenantController {

    @Autowired
    private final TenantService tenantService;

    @GetMapping("/viewProperties/")
    public List<Property> getAllProperty(){
        log.info("Fetching all properties");
        return tenantService.getAllProperty();
    }
    @GetMapping("/search")
    public List<Property> searchProperties(
            @RequestParam(required=false) String city,
            @RequestParam(required=false) String minPrice,
            @RequestParam(required=false) String maxPrice,
            @RequestParam(required=false) String bhk){
        log.info("Searching properties with city: {}, minPrice: {}, maxPrice: {}, bhk: {}", city, minPrice, maxPrice, bhk);
        return tenantService.searchProperties(city,minPrice,maxPrice,bhk);
    }


}
