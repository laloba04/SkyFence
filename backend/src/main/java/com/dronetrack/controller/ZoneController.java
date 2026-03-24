package com.dronetrack.controller;

import com.dronetrack.model.RestrictedZone;
import com.dronetrack.repository.RestrictedZoneRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@Tag(name = "Zones", description = "Zonas restringidas del espacio aéreo")
public class ZoneController {

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
    public RestrictedZone create(@RequestBody RestrictedZone zone) {
        return zoneRepository.save(zone);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una zona restringida por ID")
    public void delete(@PathVariable Long id) {
        zoneRepository.deleteById(id);
    }
}
