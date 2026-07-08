package com.skyfence.service;

import com.skyfence.model.Aircraft;
import com.skyfence.util.IcaoCountry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class FlightDataService {

    private static final Logger log = LoggerFactory.getLogger(FlightDataService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final long CACHE_TTL_MS = 60_000;

    private final WebClient webClient;
    private final GeofenceService geofenceService;
    private final AlertService alertService;
    private final AircraftService aircraftService;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    volatile List<Aircraft> cachedAircraft = new ArrayList<>();
    volatile long lastFetchTime = 0;

    // Permite apagar el polling de adsb.fi cuando la fuente de datos es MQTT
    @Value("${flightdata.api.enabled:true}")
    boolean apiEnabled = true;

    public FlightDataService(
            @Value("${flightdata.api.url}") String baseUrl,
            GeofenceService geofenceService,
            AlertService alertService,
            AircraftService aircraftService,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            MeterRegistry meterRegistry) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
        this.geofenceService = geofenceService;
        this.alertService = alertService;
        this.aircraftService = aircraftService;
        this.retry = retryRegistry.retry("flightData");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("flightData");

        this.retry.getEventPublisher().onRetry(e ->
                log.warn("SECURITY ALERT: adsb.fi no responde, reintentando... (intento {}/3)",
                        e.getNumberOfRetryAttempts() + 1));
        this.circuitBreaker.getEventPublisher()
                .onStateTransition(e -> log.warn("SECURITY ALERT: Estado de conexión con adsb.fi → {}",
                        e.getStateTransition()));

        Gauge.builder("skyfence.aircraft.tracked", this, s -> s.cachedAircraft.size())
                .description("Aeronaves rastreadas actualmente sobre España")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${geofence.check.interval}")
    public void checkAndAlert() {
        if (!apiEnabled) return;
        // Correlation ID por ciclo de barrido: traza todos los logs de un mismo ciclo
        MDC.put("requestId", "sched-" + UUID.randomUUID());
        try {
            fetchLiveAircraft().forEach(aircraft ->
                    geofenceService.checkAircraft(aircraft).forEach(alertService::sendAlert));
        } finally {
            MDC.remove("requestId");
        }
    }

    @SuppressWarnings("unchecked")
    public List<Aircraft> fetchLiveAircraft() {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime < CACHE_TTL_MS && !cachedAircraft.isEmpty()) {
            return cachedAircraft;
        }

        Supplier<Map<String, Object>> resilientCall = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                Retry.decorateSupplier(retry, this::fetchFromApi));

        try {
            Map<String, Object> response = resilientCall.get();
            return parseAndCache(response, now);
        } catch (CallNotPermittedException e) {
            log.warn("SECURITY ALERT: Circuit breaker OPEN para adsb.fi — usando caché local ({} aeronaves)",
                    cachedAircraft.size());
            return cachedAircraft;
        } catch (Exception e) {
            log.warn("SECURITY ALERT: adsb.fi no disponible tras reintentos — usando caché local. Causa: {}",
                    e.getMessage());
            return cachedAircraft;
        }
    }

    protected Map<String, Object> fetchFromApi() {
        return webClient.get()
                .uri("/api/v2/lat/39.5/lon/-3.5/dist/250")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(TIMEOUT)
                .block();
    }

    @SuppressWarnings("unchecked")
    private List<Aircraft> parseAndCache(Map<String, Object> response, long fetchTime) {
        if (response == null || !response.containsKey("aircraft")) {
            log.warn("ADSB.fi: respuesta vacía o sin campo 'aircraft'");
            return cachedAircraft;
        }

        List<Aircraft> list = new ArrayList<>();
        for (Map<String, Object> ac : (List<Map<String, Object>>) response.get("aircraft")) {
            try {
                String icao24 = (String) ac.get("hex");
                if (icao24 == null) continue;

                String callsign = ac.get("flight") != null
                        ? ac.get("flight").toString().trim() : "N/A";
                if (callsign.isEmpty()) callsign = "N/A";

                Double lat = ac.get("lat") != null ? ((Number) ac.get("lat")).doubleValue() : null;
                Double lon = ac.get("lon") != null ? ((Number) ac.get("lon")).doubleValue() : null;
                if (lat == null || lon == null) continue;

                Object altBaro = ac.get("alt_baro");
                Double altMeters = (altBaro instanceof Number)
                        ? ((Number) altBaro).doubleValue() * 0.3048 : null;

                Double velocityMs = ac.get("gs") != null
                        ? ((Number) ac.get("gs")).doubleValue() * 0.514444 : null;

                boolean onGround = !(altBaro instanceof Number);

                list.add(new Aircraft(icao24, callsign, IcaoCountry.fromHex(icao24), lat, lon, altMeters, velocityMs, onGround));
            } catch (Exception ignored) {
            }
        }
        log.info("ADSB.fi: {} aeronaves obtenidas sobre España", list.size());
        aircraftService.upsertAll(list);
        cachedAircraft = list;
        lastFetchTime = fetchTime;
        return list;
    }

}
