package com.skyfence.controller;

import com.skyfence.dto.LoginRequest;
import com.skyfence.dto.LoginResponse;
import com.skyfence.dto.RegisterRequest;
import com.skyfence.model.Role;
import com.skyfence.model.User;
import com.skyfence.repository.UserRepository;
import com.skyfence.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authManager,
                          JwtService jwtService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest httpRequest) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        User user = (User) auth.getPrincipal();
        String token = jwtService.generateToken(user);

        MDC.put("userId", user.getUsername());
        MDC.put("ip", httpRequest.getRemoteAddr());
        MDC.put("event", "LOGIN_SUCCESS");
        log.info("Login successful for user '{}'", user.getUsername());
        MDC.clear();

        return ResponseEntity.ok(new LoginResponse(token, user.getUsername(), user.getRole().name()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex, HttpServletRequest httpRequest) {
        MDC.put("ip", httpRequest.getRemoteAddr());
        MDC.put("event", "LOGIN_FAILED");
        log.warn("Login failed from IP {}: bad credentials", httpRequest.getRemoteAddr());
        MDC.clear();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req, HttpServletRequest httpRequest) {
        if (userRepository.existsByUsername(req.getUsername())) {
            MDC.put("ip", httpRequest.getRemoteAddr());
            MDC.put("event", "REGISTER_DUPLICATE");
            log.warn("Register failed: username '{}' already exists", req.getUsername());
            MDC.clear();
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }
        Role role;
        try {
            role = req.getRole() != null ? Role.valueOf(req.getRole().toUpperCase()) : Role.OPERATOR;
        } catch (IllegalArgumentException e) {
            MDC.put("ip", httpRequest.getRemoteAddr());
            MDC.put("event", "REGISTER_INVALID_ROLE");
            log.warn("Register failed: invalid role '{}' for username '{}'", req.getRole(), req.getUsername());
            MDC.clear();
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role"));
        }
        User user = new User(req.getUsername(), passwordEncoder.encode(req.getPassword()), role);
        userRepository.save(user);

        MDC.put("userId", req.getUsername());
        MDC.put("ip", httpRequest.getRemoteAddr());
        MDC.put("event", "REGISTER_SUCCESS");
        log.info("User '{}' registered with role {}", req.getUsername(), role);
        MDC.clear();

        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }
}
