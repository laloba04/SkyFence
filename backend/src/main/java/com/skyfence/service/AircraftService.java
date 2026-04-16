package com.skyfence.service;

import com.skyfence.model.Aircraft;
import com.skyfence.repository.AircraftRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public void upsertAll(List<Aircraft> incoming) {
        List<String> icaos = incoming.stream().map(Aircraft::getIcao24).collect(Collectors.toList());
        Map<String, Aircraft> existing = aircraftRepository.findByIcao24In(icaos).stream()
                .collect(Collectors.toMap(Aircraft::getIcao24, Function.identity()));

        List<Aircraft> toSave = incoming.stream().map(a -> {
            Aircraft entity = existing.getOrDefault(a.getIcao24(), a);
            if (entity != a) {
                entity.setCallsign(a.getCallsign());
                entity.setOriginCountry(a.getOriginCountry());
                entity.setLatitude(a.getLatitude());
                entity.setLongitude(a.getLongitude());
                entity.setAltitude(a.getAltitude());
                entity.setVelocity(a.getVelocity());
                entity.setOnGround(a.getOnGround());
            }
            return entity;
        }).collect(Collectors.toList());

        aircraftRepository.saveAll(toSave);
    }
}
