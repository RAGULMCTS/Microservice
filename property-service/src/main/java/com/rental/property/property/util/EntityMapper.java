package com.rental.property.property.util;

import com.rental.property.property.dto.PropertyDto;
import com.rental.property.property.entity.Address;
import com.rental.property.property.entity.Property;

import org.springframework.stereotype.Component;

@Component
public class EntityMapper {


    public Property convertPropDtoToProperty(PropertyDto propertyDto) {
        Address address = new Address(propertyDto.getAddress().getStreetName(), propertyDto.getAddress().getCity(),
                propertyDto.getAddress().getState(), propertyDto.getAddress().getPinCode());
        return Property.builder()


                .address(address)
                .propertyType(propertyDto.getPropertyType())
                .bhk(propertyDto.getBhk())
                .rentAmount(propertyDto.getRentAmount())
                .availabilityStatus(propertyDto.getAvailabilityStatus())
                .description(propertyDto.getDescription())
                .build();
    }

    public PropertyDto covertPropToPropDto(Property property){
        Address address = new Address(property.getAddress().getStreetName(), property.getAddress().getCity(),
                property.getAddress().getState(), property.getAddress().getPinCode());

        return PropertyDto.builder()


                .address(address)
                .propertyType(property.getPropertyType())
                .bhk(property.getBhk())
                .rentAmount(property.getRentAmount())
                .availabilityStatus(property.getAvailabilityStatus())
                .description(property.getDescription())
                .build();
    }

}
