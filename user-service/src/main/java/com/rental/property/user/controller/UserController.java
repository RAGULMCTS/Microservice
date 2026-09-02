package com.rental.property.user.controller;

import com.rental.property.user.dto.UserDto;
import com.rental.property.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    @PutMapping("/updateProfile/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable  Long id,@Valid @RequestBody UserDto userDto){
        log.info("Updating user profile for user ID: {} with details: {}", id, userDto);
            UserDto userDto2=userService.updateProfile(id,userDto);
            return new ResponseEntity<>(userDto2,HttpStatus.OK);
    }

    @GetMapping("/viewProfile/{userId}")
    public ResponseEntity<UserDto> viewProfile(@PathVariable Long userId){
        log.info("Fetching profile details for user ID: {}", userId);
        UserDto userDto3= userService.viewProfile(userId);
        return new ResponseEntity<>(userDto3,HttpStatus.OK);
    }

}
