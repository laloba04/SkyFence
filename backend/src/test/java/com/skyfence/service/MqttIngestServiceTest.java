package com.skyfence.service;

import com.skyfence.model.Aircraft;
import com.skyfence.model.Alert;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttIngestServiceTest {

    @Mock
    GeofenceService geofenceService;

    @Mock
    AlertService alertService;

    @Mock
    AircraftService aircraftService;

    SimpleMeterRegistry meterRegistry;
    MqttIngestService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new MqttIngestService(
                "tcp://localhost:1883", "skyfence/sensors/aircraft", "test-client",
                "", "", 1,
                geofenceService, alertService, aircraftService, meterRegistry);
    }

    private double counter(String result) {
        return meterRegistry.counter("skyfence.mqtt.messages", "result", result).count();
    }

    @Test
    void handleMessage_processesValidReadingThroughPipeline() {
        when(geofenceService.checkAircraft(any())).thenReturn(List.of());

        service.handleMessage("""
                {"icao24":"342266","callsign":"DRON01","latitude":40.4983,
                 "longitude":-3.5676,"altitude":120.5,"velocity":12.3,"onGround":false}
                """);

        ArgumentCaptor<List<Aircraft>> captor = ArgumentCaptor.forClass(List.class);
        verify(aircraftService).upsertAll(captor.capture());
        Aircraft aircraft = captor.getValue().get(0);
        assertEquals("342266", aircraft.getIcao24());
        assertEquals("DRON01", aircraft.getCallsign());
        assertEquals("Spain", aircraft.getOriginCountry());
        assertEquals(40.4983, aircraft.getLatitude());
        assertEquals(1.0, counter("processed"));
    }

    @Test
    void handleMessage_sendsAlertsWhenAircraftEntersZone() {
        Alert alert = new Alert("342266", "DRON01", "Aeropuerto Madrid-Barajas", "AIRPORT", 1.2, "HIGH");
        when(geofenceService.checkAircraft(any())).thenReturn(List.of(alert));

        service.handleMessage("""
                {"icao24":"342266","latitude":40.4983,"longitude":-3.5676}
                """);

        verify(alertService).sendAlert(alert);
    }

    @Test
    void handleMessage_defaultsOptionalFields() {
        when(geofenceService.checkAircraft(any())).thenReturn(List.of());

        service.handleMessage("""
                {"icao24":"zz9999","latitude":10.0,"longitude":20.0}
                """);

        ArgumentCaptor<List<Aircraft>> captor = ArgumentCaptor.forClass(List.class);
        verify(aircraftService).upsertAll(captor.capture());
        Aircraft aircraft = captor.getValue().get(0);
        assertEquals("SENSOR", aircraft.getCallsign());
        assertEquals(false, aircraft.getOnGround());
        assertEquals("Unknown", aircraft.getOriginCountry());
    }

    @Test
    void handleMessage_rejectsInvalidJson() {
        service.handleMessage("esto no es json {");

        verifyNoInteractions(aircraftService, geofenceService, alertService);
        assertEquals(1.0, counter("invalid"));
    }

    @Test
    void handleMessage_rejectsMissingIcao24() {
        service.handleMessage("""
                {"latitude":40.0,"longitude":-3.0}
                """);

        verifyNoInteractions(aircraftService);
        assertEquals(1.0, counter("invalid"));
    }

    @Test
    void handleMessage_rejectsMaliciousIcao24() {
        service.handleMessage("""
                {"icao24":"abc; DROP TABLE aircraft","latitude":40.0,"longitude":-3.0}
                """);

        verifyNoInteractions(aircraftService);
        assertEquals(1.0, counter("invalid"));
    }

    @Test
    void handleMessage_rejectsOutOfRangeCoordinates() {
        service.handleMessage("""
                {"icao24":"342266","latitude":95.0,"longitude":-3.0}
                """);
        service.handleMessage("""
                {"icao24":"342266","latitude":40.0,"longitude":190.0}
                """);

        verify(aircraftService, never()).upsertAll(any());
        assertEquals(2.0, counter("invalid"));
    }

    @Test
    void handleMessage_ignoresUnknownJsonFields() {
        when(geofenceService.checkAircraft(any())).thenReturn(List.of());

        service.handleMessage("""
                {"icao24":"342266","latitude":40.0,"longitude":-3.0,"sensorId":"garden-01","battery":87}
                """);

        assertEquals(1.0, counter("processed"));
    }
}
