package com.rental.property.user;

import com.rental.property.user.entity.Role;
import com.rental.property.user.entity.User;
import com.rental.property.user.repo.RoleRepository;
import com.rental.property.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class UserServiceApplication implements CommandLineRunner {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        log.info("After running  my app");
    }

    @Override
    public void run(String... args) throws Exception {
        // Create or fetch roles
        Role adminRole = roleRepository.findByName("ROLE_LANDLORD")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("ROLE_LANDLORD");
                    return roleRepository.save(newRole);
                });

        Role userRole = roleRepository.findByName("ROLE_TENANT")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("ROLE_TENANT");
                    return roleRepository.save(newRole);
                });

        // Create admin user
        if (!userRepository.findByUsername("landlord").isPresent()) {
            User landlord = new User();
            landlord.setUsername("landlord");
            landlord.setPassword(passwordEncoder.encode("landlord123"));
            landlord.setRoles(Set.of(adminRole));
            landlord.setEmail("Ragul.M5@cognizant.com");
            landlord.setFirstName("RAGUL");
            landlord.setLastName("M");
            landlord.setMobileNo(876534567L);
            landlord.setRole(1L);
            userRepository.save(landlord);
        }

        // Create regular user
        if (!userRepository.findByUsername("tenant").isPresent()) {
            User tenant = new User();
            tenant.setUsername("tenant");
            tenant.setPassword(passwordEncoder.encode("tenant123"));
            tenant.setRoles(Set.of(userRole));
            tenant.setEmail("Saveetha.S@cognizant.com");
            tenant.setFirstName("Saveetha");
            tenant.setLastName("Sankar");
            tenant.setMobileNo(987654321L);
            tenant.setRole(2L);
            userRepository.save(tenant);
        }
    }

}
