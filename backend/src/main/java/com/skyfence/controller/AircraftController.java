package com.skyfence.controller;

import com.skyfence.model.Aircraft;
import com.skyfence.service.AircraftService;
import com.skyfence.service.FlightDataService;
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
    private final FlightDataService flightDataService;

    public AircraftController(AircraftService aircraftService, FlightDataService flightDataService) {
        this.aircraftService = aircraftService;
        this.flightDataService = flightDataService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las aeronaves guardadas en BD")
    public List<Aircraft> getAll() {
        return aircraftService.getAllAircraft();
    }

    @GetMapping("/live")
    @Operation(summary = "Aeronaves en tiempo real desde adsb.fi")
    public List<Aircraft> getLive() {
        return flightDataService.fetchLiveAircraft();
    }

    @GetMapping("/flying")
    @Operation(summary = "Aeronaves actualmente en vuelo (no en tierra)")
    public List<Aircraft> getFlying() {
        return aircraftService.getAircraftInFlight();
    }
}
