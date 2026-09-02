package com.rental.property.property.controller;

import com.rental.property.property.dto.PropertyDto;
import com.rental.property.property.entity.Property;
import com.rental.property.property.service.PropertyServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/landlord")
public class PropertyController {

    @Autowired
     private  final PropertyServiceImpl propertyServiceImpl;

    @PostMapping("/addProperty")
    public PropertyDto  addNewProperty(@Valid @RequestPart PropertyDto propertyDto,@RequestPart MultipartFile image) throws IOException {
        log.info("Adding new property: {}", propertyDto);
        return propertyServiceImpl.addNewProperty(propertyDto,image);
    }

    @GetMapping("/viewMyProperties/{ownerId}")
    public List<Property> getAllPropertyByOwnerId(@PathVariable  Long ownerId){
        log.info("Fetching all properties for owner ID: {}", ownerId);
        return propertyServiceImpl.getAllPropertyByOwnerId(ownerId);
    }

    @GetMapping("/viewProperty/{propertyId}")
    public PropertyDto getPropertyById(@PathVariable Long propertyId){
        log.info("Fetching property details for property ID: {}", propertyId);
        return propertyServiceImpl.getPropertyById(propertyId);
    }

    @PatchMapping("/updateProperty/{id}")
    public PropertyDto updateProperty(@PathVariable("id") Long propertyId,@Valid @RequestBody PropertyDto propertyDto){
        log.info("Updating property ID: {} with details: {}", propertyId, propertyDto);
        return propertyServiceImpl.updateProperty(propertyId,propertyDto);
    }

    @DeleteMapping("/deleteProperty/{id}")
    public void deleteProperty(@PathVariable("id") Long propertyId){
        log.info("Deleting property with ID: {}", propertyId);
         propertyServiceImpl.deleteProperty(propertyId);
    }

}
