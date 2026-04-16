package com.skyfence.service;

import com.skyfence.model.Aircraft;
import com.skyfence.repository.AircraftRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        // Deduplicar por ICAO: adsb.fi puede devolver el mismo hex más de una vez
        Map<String, Aircraft> incomingMap = new LinkedHashMap<>();
        for (Aircraft a : incoming) incomingMap.putIfAbsent(a.getIcao24(), a);
        List<Aircraft> unique = new ArrayList<>(incomingMap.values());

        List<String> icaos = unique.stream().map(Aircraft::getIcao24).collect(Collectors.toList());
        Map<String, Aircraft> existing = aircraftRepository.findByIcao24In(icaos).stream()
                .collect(Collectors.toMap(Aircraft::getIcao24, Function.identity(), (a, b) -> a));

        List<Aircraft> toSave = unique.stream().map(a -> {
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
