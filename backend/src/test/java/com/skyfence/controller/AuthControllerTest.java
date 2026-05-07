package com.skyfence.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyfence.model.Role;
import com.skyfence.model.User;
import com.skyfence.repository.UserRepository;
import com.skyfence.security.JwtService;
import com.skyfence.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AuthenticationManager authManager;

    @MockBean
    JwtService jwtService;

    @MockBean
    UserRepository userRepository;

    @MockBean
    PasswordEncoder passwordEncoder;

    @MockBean
    UserDetailsServiceImpl userDetailsService;

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        User user = new User("admin", "encodedPwd", Role.ADMIN);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        when(authManager.authenticate(any())).thenReturn(authToken);
        when(jwtService.generateToken(user)).thenReturn("test.jwt.token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test.jwt.token"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void register_newUser_returns200() throws Exception {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPwd");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "newuser", "password", "pass123", "role", "OPERATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void register_duplicateUsername_returns400() throws Exception {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "admin", "password", "pass123", "role", "OPERATOR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void register_invalidRole_returns400() throws Exception {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "newuser", "password", "pass123", "role", "INVALID_ROLE"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid role"));
    }

    @Test
    void register_nullRole_defaultsToOperator() throws Exception {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPwd");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "newuser", "password", "pass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }
}
