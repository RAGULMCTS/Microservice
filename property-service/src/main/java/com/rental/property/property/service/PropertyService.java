package com.rental.property.property.service;

import com.rental.property.property.dto.PropertyDto;
import com.rental.property.property.entity.Property;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public interface PropertyService {
    public  PropertyDto addNewProperty(PropertyDto propertyDto, MultipartFile image) throws IOException;

    public PropertyDto getPropertyById(Long propertyId);

    public  PropertyDto updateProperty(Long propertyId, PropertyDto propertydto);

    public  void deleteProperty(Long propertyId);


    List<Property> getAllPropertyByOwnerId(Long ownerId);
}
