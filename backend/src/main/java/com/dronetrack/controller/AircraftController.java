package com.dronetrack.controller;

import com.dronetrack.model.Aircraft;
import com.dronetrack.service.AircraftService;
import com.dronetrack.service.OpenSkyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/aircraft")
@CrossOrigin(origins = "http://localhost:5173")
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
    @Operation(summary = "Aeronaves en tiempo real desde OpenSky Network")
    public List<Aircraft> getLive() {
        return openSkyService.fetchLiveAircraft();
    }

    @GetMapping("/flying")
    @Operation(summary = "Aeronaves actualmente en vuelo (no en tierra)")
    public List<Aircraft> getFlying() {
        return aircraftService.getAircraftInFlight();
    }
}
