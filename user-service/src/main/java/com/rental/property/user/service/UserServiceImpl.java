package com.rental.property.user.service;

import com.rental.property.user.dto.UserDto;
import com.rental.property.user.entity.Role;
import com.rental.property.user.entity.User;
import com.rental.property.user.repo.RoleRepository;
import com.rental.property.user.repo.UserRepository;
import com.rental.property.user.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService{

    @Autowired
    private UserRepository userRepository;
  @Autowired
    private PasswordEncoder passwordEncoder;

  @Autowired
  private UserUtil userUtil;

  @Autowired
  private RoleRepository roleRepository;


    @Override
    public UserDto registerNewUser(UserDto userDto) {
        Set<Role> roleSet=new HashSet<>();

            Role role=roleRepository.findById(userDto.getRole()).get();
            roleSet.add(role);
            User user=User.builder().username(userDto.getUsername())
                    .password(passwordEncoder.encode(userDto.getPassword()))
                    .email(userDto.getEmail())
                    .firstName(userDto.getFirstName())
                    .lastName(userDto.getLastName())
                    .mobileNo(userDto.getMobileNo())
                    .role(userDto.getRole())
                    .roles(roleSet).build();
        userRepository.save(user);
        return userUtil.convertUserToUserDto(user);
    }

    @Override
    public UserDto updateProfile(Long id, UserDto userDto) {
        User user =userRepository.findById(id).get();
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setMobileNo(userDto.getMobileNo());
        userRepository.save(user);
        return userUtil.convertUserToUserDto(user);
    }

    @Override
    public UserDto viewProfile(Long id) {
        User user=userRepository.findById(id).get();
        return userUtil.convertUserToUserDto(user);
    }


}
