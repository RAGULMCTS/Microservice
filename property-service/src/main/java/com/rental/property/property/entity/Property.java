package com.rental.property.property.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Property  extends BaseAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long propertyId;

    // Cross-service reference to User.id in user-service; not a JPA relationship
    // since users and properties now live in separate databases.
    private Long ownerId;
    @Embedded
    private Address address;
    private String propertyType;
    private String bhk;
    private double rentAmount;
    private String availabilityStatus;
    private String description;
    @Lob
    private byte[] image1;

    @Version
    private  Long  version;

}
