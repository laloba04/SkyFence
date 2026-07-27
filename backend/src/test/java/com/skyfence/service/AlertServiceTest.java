package com.skyfence.service;

import com.skyfence.model.Alert;
import com.skyfence.repository.AlertRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    AlertRepository alertRepository;

    SimpleMeterRegistry meterRegistry;
    AlertService alertService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        alertService = new AlertService(messagingTemplate, alertRepository, meterRegistry);
        // Cooldown desactivado salvo en los tests que lo ejercitan expresamente
        alertService.cooldownMinutes = 0;
    }

    private Alert alert(String severity) {
        return new Alert("ABC123", "IBE001", "Aeropuerto Madrid-Barajas", "AIRPORT", 2.5, severity);
    }

    private Alert alert(String icao, String zone) {
        return new Alert(icao, "IBE001", zone, "AIRPORT", 2.5, "HIGH");
    }

    /** Fija el reloj del servicio en un instante concreto. */
    private void clockAt(Instant instant) {
        alertService.clock = Clock.fixed(instant, ZoneOffset.UTC);
    }

    @Test
    void sendAlert_persistsAndPublishes() {
        Alert alert = alert("HIGH");

        alertService.sendAlert(alert);

        verify(alertRepository).save(alert);
        verify(messagingTemplate).convertAndSend("/topic/alerts", alert);
    }

    @Test
    void sendAlert_incrementsCounterBySeverityAndZoneType() {
        alertService.sendAlert(alert("HIGH"));
        alertService.sendAlert(alert("HIGH"));
        alertService.sendAlert(alert("MEDIUM"));

        double high = meterRegistry.counter("skyfence.alerts",
                "severity", "HIGH", "zone_type", "AIRPORT").count();
        double medium = meterRegistry.counter("skyfence.alerts",
                "severity", "MEDIUM", "zone_type", "AIRPORT").count();

        assertEquals(2.0, high);
        assertEquals(1.0, medium);
    }

    @Test
    void sendAlert_recordsPublishLatency() {
        alertService.sendAlert(alert("HIGH"));

        assertEquals(1, meterRegistry.timer("skyfence.alert.publish").count());
    }

    private double suppressed() {
        return meterRegistry.counter("skyfence.alerts.suppressed").count();
    }

    @Test
    void sendAlert_suppressesRepeatedIntrusionWithinCooldown() {
        alertService.cooldownMinutes = 5;
        Instant t0 = Instant.parse("2026-01-01T10:00:00Z");

        clockAt(t0);
        alertService.sendAlert(alert("ABC123", "Barajas"));
        clockAt(t0.plus(Duration.ofSeconds(10)));   // siguiente barrido
        alertService.sendAlert(alert("ABC123", "Barajas"));
        clockAt(t0.plus(Duration.ofMinutes(4)));    // aún dentro de la ventana
        alertService.sendAlert(alert("ABC123", "Barajas"));

        verify(alertRepository, times(1)).save(any());
        assertEquals(2.0, suppressed());
    }

    @Test
    void sendAlert_alertsAgainOnceCooldownExpires() {
        alertService.cooldownMinutes = 5;
        Instant t0 = Instant.parse("2026-01-01T10:00:00Z");

        clockAt(t0);
        alertService.sendAlert(alert("ABC123", "Barajas"));
        clockAt(t0.plus(Duration.ofMinutes(6)));    // pasada la ventana
        alertService.sendAlert(alert("ABC123", "Barajas"));

        verify(alertRepository, times(2)).save(any());
        assertEquals(0.0, suppressed());
    }

    @Test
    void sendAlert_cooldownIsPerAircraftAndZone() {
        alertService.cooldownMinutes = 5;
        clockAt(Instant.parse("2026-01-01T10:00:00Z"));

        alertService.sendAlert(alert("ABC123", "Barajas"));
        alertService.sendAlert(alert("XYZ789", "Barajas"));   // otra aeronave
        alertService.sendAlert(alert("ABC123", "El Prat"));   // otra zona

        verify(alertRepository, times(3)).save(any());
        assertEquals(0.0, suppressed());
    }

    @Test
    void sendAlert_cooldownDisabledWithZero() {
        alertService.cooldownMinutes = 0;
        clockAt(Instant.parse("2026-01-01T10:00:00Z"));

        alertService.sendAlert(alert("ABC123", "Barajas"));
        alertService.sendAlert(alert("ABC123", "Barajas"));

        verify(alertRepository, times(2)).save(any());
        assertEquals(0.0, suppressed());
    }
}
