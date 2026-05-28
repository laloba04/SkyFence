package com.skyfence.controller;

import com.skyfence.model.RestrictedZone;
import com.skyfence.model.Role;
import com.skyfence.model.SubscriptionStatus;
import com.skyfence.model.User;
import com.skyfence.repository.RestrictedZoneRepository;
import com.skyfence.security.JwtService;
import com.skyfence.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@WebMvcTest(controllers = ZoneController.class,
            excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@ActiveProfiles("test")
class ZoneControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    RestrictedZoneRepository zoneRepository;

    @MockBean
    JwtService jwtService;

    @MockBean
    UserDetailsServiceImpl userDetailsService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAll_shouldReturn200WithZones() throws Exception {
        RestrictedZone zone = new RestrictedZone("Aeropuerto Madrid-Barajas", "AIRPORT", 40.4983, -3.5676, 5.0);
        when(zoneRepository.findAll()).thenReturn(List.of(zone));

        mockMvc.perform(get("/api/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Aeropuerto Madrid-Barajas"))
                .andExpect(jsonPath("$[0].type").value("AIRPORT"));
    }

    @Test
    void getAll_whenEmpty_shouldReturnEmptyList() throws Exception {
        when(zoneRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_shouldReturn200WithSavedZone() throws Exception {
        RestrictedZone zone = new RestrictedZone("Base Naval de Rota", "MILITARY", 36.6412, -6.3496, 5.0);
        when(zoneRepository.save(any(RestrictedZone.class))).thenReturn(zone);

        mockMvc.perform(post("/api/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(zone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Base Naval de Rota"))
                .andExpect(jsonPath("$.type").value("MILITARY"));

        verify(zoneRepository, times(1)).save(any(RestrictedZone.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_shouldReturn200AndCallRepository() throws Exception {
        doNothing().when(zoneRepository).deleteById(1L);

        mockMvc.perform(delete("/api/zones/1"))
                .andExpect(status().isOk());

        verify(zoneRepository, times(1)).deleteById(1L);
    }

    @Test
    void create_freeUserAtLimit_returns403() throws Exception {
        User freeUser = new User("freeuser", "pwd", Role.OPERATOR);
        freeUser.setSubscriptionStatus(SubscriptionStatus.FREE);
        UsernamePasswordAuthenticationToken principal =
                new UsernamePasswordAuthenticationToken(freeUser, null, freeUser.getAuthorities());

        when(zoneRepository.count()).thenReturn(3L);

        RestrictedZone zone = new RestrictedZone("Nueva Zona", "AIRPORT", 40.0, -3.0, 2.0);

        mockMvc.perform(post("/api/zones")
                        .with(authentication(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zone)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Free plan limit reached"));

        verify(zoneRepository, never()).save(any());
    }

    @Test
    void create_freeUserBelowLimit_returns200() throws Exception {
        User freeUser = new User("freeuser", "pwd", Role.OPERATOR);
        freeUser.setSubscriptionStatus(SubscriptionStatus.FREE);
        UsernamePasswordAuthenticationToken principal =
                new UsernamePasswordAuthenticationToken(freeUser, null, freeUser.getAuthorities());

        RestrictedZone zone = new RestrictedZone("Nueva Zona", "AIRPORT", 40.0, -3.0, 2.0);
        when(zoneRepository.count()).thenReturn(2L);
        when(zoneRepository.save(any(RestrictedZone.class))).thenReturn(zone);

        mockMvc.perform(post("/api/zones")
                        .with(authentication(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zone)))
                .andExpect(status().isOk());

        verify(zoneRepository, times(1)).save(any());
    }

}
