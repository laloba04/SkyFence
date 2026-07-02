package com.skyfence.controller;

import com.skyfence.dto.ZoneRequest;
import com.skyfence.model.RestrictedZone;
import com.skyfence.model.SubscriptionStatus;
import com.skyfence.model.User;
import com.skyfence.repository.RestrictedZoneRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/zones")
@Tag(name = "Zones", description = "Zonas restringidas del espacio aéreo")
public class ZoneController {

    static final int FREE_ZONE_LIMIT = 3;

    private final RestrictedZoneRepository zoneRepository;

    public ZoneController(RestrictedZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    @GetMapping
    @Operation(summary = "Lista todas las zonas restringidas")
    public List<RestrictedZone> getAll() {
        return zoneRepository.findAll();
    }

    @PostMapping
    @Operation(summary = "Añade una nueva zona restringida")
    public ResponseEntity<?> create(@RequestBody ZoneRequest request,
                                    @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        if (user.getSubscriptionStatus() == SubscriptionStatus.FREE
                && zoneRepository.count() >= FREE_ZONE_LIMIT) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Free plan limit reached",
                    "message", "Upgrade to Pro to create more than " + FREE_ZONE_LIMIT + " zones"
            ));
        }
        RestrictedZone zone = new RestrictedZone(
                request.getName(), request.getType(),
                request.getLatitude(), request.getLongitude(), request.getRadiusKm());
        return ResponseEntity.ok(zoneRepository.save(zone));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una zona restringida por ID")
    public void delete(@PathVariable Long id) {
        zoneRepository.deleteById(id);
    }
}
