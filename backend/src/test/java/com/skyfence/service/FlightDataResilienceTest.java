package com.skyfence.service;

import com.skyfence.model.Aircraft;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FlightDataResilienceTest {

    private RetryRegistry retryRegistry;
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ZERO)
                .build();
        retryRegistry = RetryRegistry.of(retryConfig);

        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(3)
                .failureRateThreshold(100)
                .waitDurationInOpenState(Duration.ofMillis(100))
                .build();
        circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig);
    }

    @Test
    void whenApiFailsAllAttempts_retriesThreeTimes() {
        AtomicInteger callCount = new AtomicInteger(0);

        FlightDataService service = new FlightDataService(
                "http://localhost", null, null, null,
                retryRegistry, circuitBreakerRegistry) {
            @Override
            protected Map<String, Object> fetchFromApi() {
                callCount.incrementAndGet();
                throw new RuntimeException("API down");
            }
        };

        List<Aircraft> result = service.fetchLiveAircraft();

        assertEquals(3, callCount.get(), "Debe reintentar 3 veces antes de rendirse");
        assertTrue(result.isEmpty(), "Debe retornar caché vacía si no hay datos previos");
    }

    @Test
    void whenApiFailsAllAttempts_returnsCachedAircraft() {
        Aircraft cached = new Aircraft("ABC123", "IBE001", "Spain", 40.0, -3.0, 8000.0, 250.0, false);

        FlightDataService service = new FlightDataService(
                "http://localhost", null, null, null,
                retryRegistry, circuitBreakerRegistry) {
            @Override
            protected Map<String, Object> fetchFromApi() {
                throw new RuntimeException("API down");
            }
        };

        service.cachedAircraft = List.of(cached);
        service.lastFetchTime = 0;

        List<Aircraft> result = service.fetchLiveAircraft();

        assertEquals(1, result.size());
        assertEquals("IBE001", result.get(0).getCallsign());
    }

    @Test
    void whenCircuitBreakerOpens_doesNotCallApi() {
        AtomicInteger callCount = new AtomicInteger(0);

        FlightDataService service = new FlightDataService(
                "http://localhost", null, null, null,
                retryRegistry, circuitBreakerRegistry) {
            @Override
            protected Map<String, Object> fetchFromApi() {
                callCount.incrementAndGet();
                throw new RuntimeException("API down");
            }
        };

        // 3 fallos consecutivos llenan la ventana y abren el circuito
        for (int i = 0; i < 3; i++) {
            service.lastFetchTime = 0;
            service.fetchLiveAircraft();
        }

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("flightData");
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        int callsBeforeOpen = callCount.get();
        service.lastFetchTime = 0;
        service.fetchLiveAircraft();

        assertEquals(callsBeforeOpen, callCount.get(), "Con el circuito abierto no debe llamar a la API");
    }

    @Test
    void whenCircuitBreakerHalfOpen_allowsProbeCall() throws InterruptedException {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(1)
                .failureRateThreshold(100)
                .waitDurationInOpenState(Duration.ofMillis(50))
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .permittedNumberOfCallsInHalfOpenState(1)
                .build();
        CircuitBreakerRegistry fastRegistry = CircuitBreakerRegistry.of(cbConfig);

        AtomicInteger callCount = new AtomicInteger(0);

        FlightDataService service = new FlightDataService(
                "http://localhost", null, null, null,
                RetryRegistry.of(RetryConfig.custom().maxAttempts(1).waitDuration(Duration.ZERO).build()),
                fastRegistry) {
            @Override
            protected Map<String, Object> fetchFromApi() {
                callCount.incrementAndGet();
                throw new RuntimeException("API down");
            }
        };

        // Abre el circuito
        service.fetchLiveAircraft();
        CircuitBreaker cb = fastRegistry.circuitBreaker("flightData");
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        // Espera transición a HALF_OPEN
        Thread.sleep(150);
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());

        int callsBeforeProbe = callCount.get();
        service.lastFetchTime = 0;
        service.fetchLiveAircraft();

        assertTrue(callCount.get() > callsBeforeProbe, "Debe permitir llamada de sondeo en HALF_OPEN");
    }
}
