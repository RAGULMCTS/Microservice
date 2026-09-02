package com.rental.property.user.controller;

import com.rental.property.user.dto.UserDto;
import com.rental.property.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerNewUser(@Valid @RequestBody UserDto userDto){
        log.info("Registering new user: {}", userDto);
        UserDto userDto1=userService.registerNewUser(userDto);
        return new ResponseEntity<>(userDto1, HttpStatus.OK);
    }

}
