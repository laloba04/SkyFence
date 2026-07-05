package com.skyfence.service;

import com.skyfence.model.Alert;
import com.skyfence.repository.AlertRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AlertRepository alertRepository;
    private final MeterRegistry meterRegistry;
    private final Timer publishTimer;

    public AlertService(SimpMessagingTemplate messagingTemplate, AlertRepository alertRepository,
                        MeterRegistry meterRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.alertRepository = alertRepository;
        this.meterRegistry = meterRegistry;
        this.publishTimer = Timer.builder("skyfence.alert.publish")
                .description("Latencia de persistir y publicar una alerta por WebSocket")
                .register(meterRegistry);
    }

    public void sendAlert(Alert alert) {
        publishTimer.record(() -> {
            alertRepository.save(alert);
            messagingTemplate.convertAndSend("/topic/alerts", alert);
        });
        Counter.builder("skyfence.alerts")
                .description("Alertas de intrusión generadas")
                .tag("severity", alert.getSeverity() != null ? alert.getSeverity() : "UNKNOWN")
                .tag("zone_type", alert.getZoneType() != null ? alert.getZoneType() : "UNKNOWN")
                .register(meterRegistry)
                .increment();
    }
}
