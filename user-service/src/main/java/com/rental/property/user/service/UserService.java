package com.rental.property.user.service;

import com.rental.property.user.dto.UserDto;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

    UserDto registerNewUser(UserDto userDto);

    UserDto updateProfile(Long id, UserDto userDto);

    UserDto viewProfile(Long userId);
}
