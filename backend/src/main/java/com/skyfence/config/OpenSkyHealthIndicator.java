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

    public OpenSkyHealthIndicator(@Value("${opensky.api.url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Health health() {
        try {
            webClient.get()
                    .uri("/states/all?lamin=40&lamax=41&lomin=-4&lomax=-3")
                    .retrieve()
                    .toBodilessEntity()
                    .block(TIMEOUT);

            return Health.up()
                    .withDetail("service", "OpenSky Network API")
                    .withDetail("status", "reachable")
                    .build();
        } catch (Exception e) {
            Health.Builder builder = e.getMessage() != null && e.getMessage().contains("429") 
                ? Health.up().withDetail("status", "rate_limited")
                : Health.down();
            
            return builder
                    .withDetail("service", "OpenSky Network API")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
