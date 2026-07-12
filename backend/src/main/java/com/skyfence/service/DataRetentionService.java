package com.skyfence.service;

import com.skyfence.repository.AircraftRepository;
import com.skyfence.repository.AlertRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Política de retención de datos (issue #42): purga diaria de alertas antiguas
 * y de aeronaves que llevan días sin observarse, para que la BD del free tier
 * (1 GB) no crezca sin límite. Umbrales parametrizados por variables de entorno.
 */
@Service
public class DataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

    private final AlertRepository alertRepository;
    private final AircraftRepository aircraftRepository;
    private final Counter purgedAlerts;
    private final Counter purgedAircraft;

    @Value("${skyfence.retention.alerts-days:30}")
    int alertRetentionDays = 30;

    @Value("${skyfence.retention.aircraft-days:7}")
    int aircraftRetentionDays = 7;

    public DataRetentionService(AlertRepository alertRepository,
                                AircraftRepository aircraftRepository,
                                MeterRegistry meterRegistry) {
        this.alertRepository = alertRepository;
        this.aircraftRepository = aircraftRepository;
        this.purgedAlerts = Counter.builder("skyfence.retention.purged")
                .tag("entity", "alerts")
                .description("Alertas eliminadas por la política de retención")
                .register(meterRegistry);
        this.purgedAircraft = Counter.builder("skyfence.retention.purged")
                .tag("entity", "aircraft")
                .description("Aeronaves eliminadas por la política de retención")
                .register(meterRegistry);
    }

    @Scheduled(cron = "${skyfence.retention.cron:0 0 4 * * *}")
    @Transactional
    public void purgeOldData() {
        MDC.put("requestId", "retention-" + UUID.randomUUID());
        try {
            long alerts = alertRepository.deleteByDetectedAtBefore(
                    LocalDateTime.now().minusDays(alertRetentionDays));
            long aircraft = aircraftRepository.deleteByLastSeenBefore(
                    LocalDateTime.now().minusDays(aircraftRetentionDays));
            purgedAlerts.increment(alerts);
            purgedAircraft.increment(aircraft);
            log.info("Retención: purgadas {} alertas (> {} días) y {} aeronaves sin ver (> {} días)",
                    alerts, alertRetentionDays, aircraft, aircraftRetentionDays);
        } finally {
            MDC.remove("requestId");
        }
    }
}
