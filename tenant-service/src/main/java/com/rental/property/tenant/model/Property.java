package com.rental.property.tenant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Plain data-carrier mirroring property-service's Property entity so the JSON
 * shape returned to tenants (GET /api/v1/tenant/**) is unchanged from the monolith;
 * tenant-service holds no property data itself, it only relays property-service's response.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Property {
    private Long propertyId;
    private Long ownerId;
    private Address address;
    private String propertyType;
    private String bhk;
    private double rentAmount;
    private String availabilityStatus;
    private String description;
    private byte[] image1;
    private Long version;
    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
}
