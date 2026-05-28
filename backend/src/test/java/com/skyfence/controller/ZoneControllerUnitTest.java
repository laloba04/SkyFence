package com.skyfence.controller;

import com.skyfence.dto.ZoneRequest;
import com.skyfence.model.RestrictedZone;
import com.skyfence.model.Role;
import com.skyfence.model.SubscriptionStatus;
import com.skyfence.model.User;
import com.skyfence.repository.RestrictedZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoneControllerUnitTest {

    @Mock
    RestrictedZoneRepository zoneRepository;

    @InjectMocks
    ZoneController controller;

    private RestrictedZone zone() {
        return new RestrictedZone("Test", "AIRPORT", 40.0, -3.0, 2.0);
    }

    private ZoneRequest request() {
        ZoneRequest r = new ZoneRequest();
        r.setName("Test"); r.setType("AIRPORT");
        r.setLatitude(40.0); r.setLongitude(-3.0); r.setRadiusKm(2.0);
        return r;
    }

    @Test
    void create_proUserAboveLimit_returns200() {
        User proUser = new User("prouser", "pwd", Role.ADMIN);
        proUser.setSubscriptionStatus(SubscriptionStatus.PRO);
        when(zoneRepository.save(any())).thenReturn(zone());

        ResponseEntity<?> result = controller.create(request(), proUser);

        assertEquals(200, result.getStatusCode().value());
        verify(zoneRepository).save(any());
        verify(zoneRepository, never()).count();
    }

    @Test
    void create_nullUser_returns200() {
        when(zoneRepository.save(any())).thenReturn(zone());

        ResponseEntity<?> result = controller.create(request(), null);

        assertEquals(200, result.getStatusCode().value());
        verify(zoneRepository).save(any());
        verify(zoneRepository, never()).count();
    }

    @Test
    void create_freeUserAtExactLimit_returns403() {
        User freeUser = new User("free", "pwd", Role.OPERATOR);
        freeUser.setSubscriptionStatus(SubscriptionStatus.FREE);
        when(zoneRepository.count()).thenReturn(3L);

        ResponseEntity<?> result = controller.create(request(), freeUser);

        assertEquals(403, result.getStatusCode().value());
        verify(zoneRepository, never()).save(any());
    }

    @Test
    void create_freeUserBelowLimit_returns200() {
        User freeUser = new User("free", "pwd", Role.OPERATOR);
        freeUser.setSubscriptionStatus(SubscriptionStatus.FREE);
        when(zoneRepository.count()).thenReturn(2L);
        when(zoneRepository.save(any())).thenReturn(zone());

        ResponseEntity<?> result = controller.create(request(), freeUser);

        assertEquals(200, result.getStatusCode().value());
        verify(zoneRepository).save(any());
    }
}
