package com.rental.property.tenant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Plain data-carrier mirroring property-service's Address embeddable so the JSON
 * shape returned to tenants is unchanged; tenant-service has no database of its own.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Address {
    private String streetName;
    private String city;
    private String state;
    private Long pinCode;
}
