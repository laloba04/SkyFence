package com.skyfence.controller;

import com.skyfence.model.Alert;
import com.skyfence.model.RestrictedZone;
import com.skyfence.repository.RestrictedZoneRepository;
import com.skyfence.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/simulate")
@Tag(name = "Simulation", description = "Simulación de intrusiones para demos y testing")
public class SimulationController {

    private final RestrictedZoneRepository zoneRepository;
    private final AlertService alertService;

    public SimulationController(RestrictedZoneRepository zoneRepository, AlertService alertService) {
        this.zoneRepository = zoneRepository;
        this.alertService = alertService;
    }

    @PostMapping
    @Operation(summary = "Simular una intrusión en una zona restringida",
               description = "Crea un aircraft ficticio dentro de la zona indicada, persiste la alerta y la emite por WebSocket.")
    public Alert simulate(@RequestParam Long zoneId) {
        RestrictedZone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zona no encontrada"));

        // Posicionar el aircraft simulado al 25% del radio → severidad HIGH (< 50%)
        double fraction = 0.25;
        double distanceKm = zone.getRadiusKm() * fraction;
        // Desplazamiento en grados: ~111 km por grado de latitud
        double deltaLat = distanceKm / 111.0;

        int randomSuffix = (int) (Math.random() * 9000) + 1000;
        String icao     = "SIM" + randomSuffix;
        String callsign = "UAV-" + randomSuffix;

        Alert alert = new Alert(icao, callsign, zone.getName(), zone.getType(),
                Math.round(distanceKm * 100.0) / 100.0, "HIGH");

        alertService.sendAlert(alert); // persiste en BD y publica en /topic/alerts
        return alert;
    }
}
