package com.dronetrack.service;

import com.dronetrack.model.Aircraft;
import com.dronetrack.model.Alert;
import com.dronetrack.model.RestrictedZone;
import com.dronetrack.repository.RestrictedZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class GeofenceServiceTest {

    @Mock
    private RestrictedZoneRepository zoneRepository;

    @InjectMocks
    private GeofenceService geofenceService;

    private RestrictedZone madridAirport;

    @BeforeEach
    void setUp() {
        madridAirport = new RestrictedZone("Aeropuerto Madrid-Barajas", "AIRPORT", 40.4983, -3.5676, 5.0);
    }

    @Test
    void checkAircraft_whenInsideZone_shouldGenerateAlert() {
        Aircraft aircraft = new Aircraft("abc123", "IBE001", "Spain", 40.510, -3.560, 500.0, 200.0, false);
        when(zoneRepository.findAll()).thenReturn(List.of(madridAirport));

        List<Alert> alerts = geofenceService.checkAircraft(aircraft);

        assertFalse(alerts.isEmpty());
        assertEquals("Aeropuerto Madrid-Barajas", alerts.get(0).getZoneName());
    }

    @Test
    void checkAircraft_whenOutsideZone_shouldNotGenerateAlert() {
        Aircraft aircraft = new Aircraft("xyz789", "VLG123", "Spain", 39.489, -0.481, 8000.0, 250.0, false);
        when(zoneRepository.findAll()).thenReturn(List.of(madridAirport));

        assertTrue(geofenceService.checkAircraft(aircraft).isEmpty());
    }

    @Test
    void checkAircraft_whenNoCoordinates_shouldReturnEmpty() {
        Aircraft aircraft = new Aircraft("null1", "N/A", "Spain", null, null, null, null, false);

        List<Alert> alerts = geofenceService.checkAircraft(aircraft);

        assertTrue(alerts.isEmpty());
        verify(zoneRepository, never()).findAll();
    }

    @Test
    void checkAircraft_whenVeryClose_shouldBeHighSeverity() {
        Aircraft aircraft = new Aircraft("close1", "IBE002", "Spain", 40.499, -3.568, 100.0, 50.0, false);
        when(zoneRepository.findAll()).thenReturn(List.of(madridAirport));

        List<Alert> alerts = geofenceService.checkAircraft(aircraft);

        assertFalse(alerts.isEmpty());
        assertEquals("HIGH", alerts.get(0).getSeverity());
    }

    @Test
    void checkAircraft_whenInsideMultipleZones_shouldGenerateMultipleAlerts() {
        RestrictedZone torrejon = new RestrictedZone("Base Aérea Torrejón", "MILITARY", 40.4967, -3.4456, 10.0);
        Aircraft aircraft = new Aircraft("multi1", "TEST", "Spain", 40.498, -3.500, 500.0, 150.0, false);
        when(zoneRepository.findAll()).thenReturn(Arrays.asList(madridAirport, torrejon));

        assertEquals(2, geofenceService.checkAircraft(aircraft).size());
    }

    @Test
    void calculateDistance_madridToBarcelona_shouldBeApprox505km() {
        double distance = geofenceService.calculateDistance(40.4168, -3.7038, 41.3828, 2.1769);
        assertTrue(distance > 490 && distance < 520, "Madrid-Barcelona debe estar entre 490 y 520 km");
    }

    @Test
    void checkAircraft_whenNoZones_shouldReturnEmpty() {
        Aircraft aircraft = new Aircraft("abc1", "IBE", "Spain", 40.498, -3.567, 500.0, 200.0, false);
        when(zoneRepository.findAll()).thenReturn(List.of());

        assertTrue(geofenceService.checkAircraft(aircraft).isEmpty());
    }
}
