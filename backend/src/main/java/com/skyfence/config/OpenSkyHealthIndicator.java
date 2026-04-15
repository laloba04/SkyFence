package com.skyfence.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Custom health indicator that checks connectivity to the OpenSky Network API.
 * Reports UP if the API responds within the timeout, DOWN otherwise.
 */
@Component("opensky")
public class OpenSkyHealthIndicator implements HealthIndicator {

    private final WebClient webClient;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    public OpenSkyHealthIndicator(@Value("${flightdata.api.url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Health health() {
        try {
            webClient.get()
                    .uri("/api/v2/lat/39.5/lon/-3.5/dist/1")
                    .retrieve()
                    .toBodilessEntity()
                    .block(TIMEOUT);

            return Health.up()
                    .withDetail("service", "adsb.fi API")
                    .withDetail("status", "reachable")
                    .build();
        } catch (Exception e) {
            return Health.unknown()
                    .withDetail("service", "adsb.fi API")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
