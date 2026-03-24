package com.dronetrack.service;

import com.dronetrack.model.Aircraft;
import com.dronetrack.repository.AircraftRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AircraftService {

    private final AircraftRepository aircraftRepository;

    public AircraftService(AircraftRepository aircraftRepository) {
        this.aircraftRepository = aircraftRepository;
    }

    public List<Aircraft> getAllAircraft() {
        return aircraftRepository.findAll();
    }

    public List<Aircraft> getAircraftInFlight() {
        return aircraftRepository.findByOnGroundFalse();
    }
}
