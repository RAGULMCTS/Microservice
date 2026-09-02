package com.rental.property.property.service;

import com.rental.property.property.dto.PropertyDto;
import com.rental.property.property.entity.Property;
import com.rental.property.property.exception.PropertyNotFoundException;
import com.rental.property.property.repo.PropertyRepository;
import com.rental.property.property.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
@RequiredArgsConstructor
@Service
public class PropertyServiceImpl  implements PropertyService{
    @Autowired
    private  final PropertyRepository propertyRepository;

    @Autowired
    private  final EntityMapper entityMapper;

    @Override
    public PropertyDto addNewProperty(PropertyDto propertyDto, MultipartFile image) throws IOException {
        Property propObj=entityMapper.convertPropDtoToProperty(propertyDto);
        propObj.setImage1(image.getBytes());
        Property savedProp= propertyRepository.save(propObj);
        return entityMapper.covertPropToPropDto(savedProp);
    }



    @Override
    public PropertyDto getPropertyById(Long propertyId) {
        Property property=propertyRepository.findById(propertyId).orElseThrow(()->new PropertyNotFoundException("No " +
                "such " +
                "property"));
        return entityMapper.covertPropToPropDto(property);
    }

    @Override
    public PropertyDto updateProperty(Long propertyId, PropertyDto propertyDto) {

        Property existing=propertyRepository.findById(propertyId).orElseThrow();
        existing.setAddress(propertyDto.getAddress());
        existing.setDescription(propertyDto.getDescription());
        existing.setAvailabilityStatus(propertyDto.getAvailabilityStatus());
        existing.setRentAmount(propertyDto.getRentAmount());
        propertyRepository.save(existing);
        return entityMapper.covertPropToPropDto(existing);

    }
    @Override
    public void deleteProperty(Long propertyId) {

        propertyRepository.deleteById(propertyId);
    }


    @Override
    public List<Property> getAllPropertyByOwnerId(Long ownerId) {
        return  propertyRepository.findPropertyByOwnerId(ownerId);
    }
}
