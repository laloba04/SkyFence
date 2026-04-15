package com.skyfence.controller;

import com.skyfence.model.Aircraft;
import com.skyfence.service.AircraftService;
import com.skyfence.service.OpenSkyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/aircraft")
@Tag(name = "Aircraft", description = "Gestión y monitorización de aeronaves")
public class AircraftController {

    private final AircraftService aircraftService;
    private final OpenSkyService openSkyService;

    public AircraftController(AircraftService aircraftService, OpenSkyService openSkyService) {
        this.aircraftService = aircraftService;
        this.openSkyService = openSkyService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las aeronaves guardadas en BD")
    public List<Aircraft> getAll() {
        return aircraftService.getAllAircraft();
    }

    @GetMapping("/live")
    @Operation(summary = "Aeronaves en tiempo real desde adsb.fi")
    public List<Aircraft> getLive() {
        return openSkyService.fetchLiveAircraft();
    }

    @GetMapping("/flying")
    @Operation(summary = "Aeronaves actualmente en vuelo (no en tierra)")
    public List<Aircraft> getFlying() {
        return aircraftService.getAircraftInFlight();
    }
}
