package com.skyfence.service;

import com.skyfence.repository.AircraftRepository;
import com.skyfence.repository.AlertRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataRetentionServiceTest {

    @Mock
    AlertRepository alertRepository;

    @Mock
    AircraftRepository aircraftRepository;

    SimpleMeterRegistry meterRegistry;
    DataRetentionService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new DataRetentionService(alertRepository, aircraftRepository, meterRegistry);
        service.alertRetentionDays = 30;
        service.aircraftRetentionDays = 7;
    }

    @Test
    void purgesWithConfiguredCutoffs() {
        when(alertRepository.deleteByDetectedAtBefore(any())).thenReturn(120);
        when(aircraftRepository.deleteByLastSeenBefore(any())).thenReturn(35);

        service.purgeOldData();

        ArgumentCaptor<LocalDateTime> alertCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> aircraftCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(alertRepository).deleteByDetectedAtBefore(alertCutoff.capture());
        verify(aircraftRepository).deleteByLastSeenBefore(aircraftCutoff.capture());

        LocalDateTime now = LocalDateTime.now();
        assertTrue(alertCutoff.getValue().isAfter(now.minusDays(30).minusMinutes(1))
                && alertCutoff.getValue().isBefore(now.minusDays(30).plusMinutes(1)),
                "el corte de alertas debe ser ~30 días atrás");
        assertTrue(aircraftCutoff.getValue().isAfter(now.minusDays(7).minusMinutes(1))
                && aircraftCutoff.getValue().isBefore(now.minusDays(7).plusMinutes(1)),
                "el corte de aeronaves debe ser ~7 días atrás");
    }

    @Test
    void incrementsPurgeCountersByEntity() {
        when(alertRepository.deleteByDetectedAtBefore(any())).thenReturn(120);
        when(aircraftRepository.deleteByLastSeenBefore(any())).thenReturn(35);

        service.purgeOldData();

        assertEquals(120.0, meterRegistry.counter("skyfence.retention.purged", "entity", "alerts").count());
        assertEquals(35.0, meterRegistry.counter("skyfence.retention.purged", "entity", "aircraft").count());
    }
}
