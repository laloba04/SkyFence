package com.dronetrack.controller;

import com.dronetrack.model.Aircraft;
import com.dronetrack.service.AircraftService;
import com.dronetrack.service.OpenSkyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AircraftController.class)
@ActiveProfiles("test")
class AircraftControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AircraftService aircraftService;

    @MockBean
    OpenSkyService openSkyService;

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
        when(openSkyService.fetchLiveAircraft()).thenReturn(List.of());
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
