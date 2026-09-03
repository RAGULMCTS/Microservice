package com.rental.property.user.controller;

import com.rental.property.user.dto.AuthRequestDTO;
import com.rental.property.user.dto.AuthResponseDTO;
import com.rental.property.user.dto.CustomUserDetails;
import com.rental.property.user.splunk.SplunkHecClient;
import com.rental.property.user.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;

    private final UserDetailsService userDetailsService;

    private final JwtUtil jwtUtil;

    private final SplunkHecClient splunkHecClient;

    @PostMapping("/login")
    @Operation(security = {@SecurityRequirement(name = "" )})
    public ResponseEntity<?> login(@RequestBody AuthRequestDTO authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            log.error("BadCredentialsException {}", e.getMessage());
            splunkHecClient.sendEvent("renthub:appevent", "renthub_events", Map.of(
                    "type", "user_login_failure",
                    "service", "user-service",
                    "username", authRequest.getUsername(),
                    "reason", "bad_credentials",
                    "requestId", String.valueOf(MDC.get("requestId"))
            ));
            return ResponseEntity.status(401).body("Invalid username or password");
        } catch (Exception e) {
            log.error("Exception {}", e.getMessage());
            return ResponseEntity.status(500).body("Authentication error: " + e.getMessage());
        }
        final CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(authRequest.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);
        AuthResponseDTO authResponseDTO =  new AuthResponseDTO(jwt,userDetails.getUsername(),userDetails.getEmail(),
                userDetails.getFirstName(),userDetails.getLastName());
        splunkHecClient.sendEvent("renthub:appevent", "renthub_events", Map.of(
                "type", "user_login_success",
                "service", "user-service",
                "username", userDetails.getUsername(),
                "requestId", String.valueOf(MDC.get("requestId"))
        ));
        return ResponseEntity.ok(authResponseDTO);
    }
}
