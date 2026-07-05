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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    }

    private Alert alert(String severity) {
        return new Alert("ABC123", "IBE001", "Aeropuerto Madrid-Barajas", "AIRPORT", 2.5, severity);
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
}
