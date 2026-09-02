package com.rental.property.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseDTO {
    private String jwt;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
}
