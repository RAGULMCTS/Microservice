package com.rental.property.tenant.service;

import com.rental.property.tenant.model.Property;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TenantService {

    List<Property> getAllProperty();


    List<Property> searchProperties(String city, String minPrice, String maxPrice, String bhk);
}
