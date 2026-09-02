package com.rental.property.tenant.service;

import com.rental.property.tenant.client.PropertyServiceClient;
import com.rental.property.tenant.model.Property;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class TenantServiceImpl  implements TenantService{

    @Autowired
    private PropertyServiceClient propertyServiceClient;

    @Override
    public List<Property> getAllProperty() {
        return propertyServiceClient.getAllProperties();
    }

    @Override
    public List<Property> searchProperties(String city, String minPrice, String maxPrice, String bhk) {
        return propertyServiceClient.searchProperties(city,minPrice,maxPrice,bhk);
    }


}
