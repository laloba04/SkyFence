package com.skyfence.controller;

import com.skyfence.model.Aircraft;
import com.skyfence.security.JwtService;
import com.skyfence.security.UserDetailsServiceImpl;
import com.skyfence.service.AircraftService;
import com.skyfence.service.FlightDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AircraftController.class,
            excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@ActiveProfiles("test")
class AircraftControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AircraftService aircraftService;

    @MockBean
    FlightDataService flightDataService;

    @MockBean
    JwtService jwtService;

    @MockBean
    UserDetailsServiceImpl userDetailsService;

    @Test
    void getAll_shouldReturn200() throws Exception {
        Aircraft a = new Aircraft("abc123", "IBE001", "Spain", 40.4, -3.7, 8000.0, 250.0, false);
        when(aircraftService.getAllAircraft()).thenReturn(List.of(a));

        mockMvc.perform(get("/api/aircraft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].callsign").value("IBE001"));
    }

    @Test
    void getLive_shouldReturn200() throws Exception {
        when(flightDataService.fetchLiveAircraft()).thenReturn(List.of());
        mockMvc.perform(get("/api/aircraft/live"))
                .andExpect(status().isOk());
    }

    @Test
    void getFlying_shouldReturn200() throws Exception {
        when(aircraftService.getAircraftInFlight()).thenReturn(List.of());
        mockMvc.perform(get("/api/aircraft/flying"))
                .andExpect(status().isOk());
    }
}
