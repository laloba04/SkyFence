package com.skyfence.service;

import com.skyfence.model.Alert;
import com.skyfence.repository.AlertRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertService {

    // A partir de este tamaño se limpian del cooldown las entradas caducadas
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final SimpMessagingTemplate messagingTemplate;
    private final AlertRepository alertRepository;
    private final MeterRegistry meterRegistry;
    private final Timer publishTimer;
    private final Counter suppressedCounter;

    // Última emisión por aeronave+zona: evita repetir la misma intrusión en
    // cada barrido del scheduler (issue #43)
    private final Map<String, Instant> recentAlerts = new ConcurrentHashMap<>();

    @Value("${skyfence.alerts.cooldown-minutes:5}")
    long cooldownMinutes = 5;

    Clock clock = Clock.systemDefaultZone();

    public AlertService(SimpMessagingTemplate messagingTemplate, AlertRepository alertRepository,
                        MeterRegistry meterRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.alertRepository = alertRepository;
        this.meterRegistry = meterRegistry;
        this.publishTimer = Timer.builder("skyfence.alert.publish")
                .description("Latencia de persistir y publicar una alerta por WebSocket")
                .register(meterRegistry);
        this.suppressedCounter = Counter.builder("skyfence.alerts.suppressed")
                .description("Alertas omitidas por el cooldown de aeronave+zona")
                .register(meterRegistry);
    }

    public void sendAlert(Alert alert) {
        if (inCooldown(alert)) {
            suppressedCounter.increment();
            return;
        }
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

    /**
     * Una intrusión ya notificada de la misma aeronave en la misma zona no se
     * repite hasta pasado el cooldown. Con cooldown <= 0 queda desactivado.
     */
    private boolean inCooldown(Alert alert) {
        if (cooldownMinutes <= 0) return false;

        String key = alert.getAircraftIcao() + "|" + alert.getZoneName();
        Instant now = clock.instant();
        Instant cutoff = now.minus(Duration.ofMinutes(cooldownMinutes));

        // El mapa solo crece con aeronaves distintas; se poda al superar el umbral
        if (recentAlerts.size() > CLEANUP_THRESHOLD) {
            recentAlerts.values().removeIf(last -> last.isBefore(cutoff));
        }

        // compute() es atómico: dos barridos simultáneos no pueden colarse ambos
        boolean[] suppressed = {false};
        recentAlerts.compute(key, (k, last) -> {
            if (last != null && last.isAfter(cutoff)) {
                suppressed[0] = true;
                return last;
            }
            return now;
        });
        return suppressed[0];
    }
}
