package com.dronetrack.service;

import com.dronetrack.model.Aircraft;
import com.dronetrack.model.Alert;
import com.dronetrack.model.RestrictedZone;
import com.dronetrack.repository.RestrictedZoneRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GeofenceService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final RestrictedZoneRepository zoneRepository;

    public GeofenceService(RestrictedZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    public List<Alert> checkAircraft(Aircraft aircraft) {
        List<Alert> alerts = new ArrayList<>();

        if (aircraft.getLatitude() == null || aircraft.getLongitude() == null) {
            return alerts;
        }

        for (RestrictedZone zone : zoneRepository.findAll()) {
            double distance = calculateDistance(
                    aircraft.getLatitude(), aircraft.getLongitude(),
                    zone.getLatitude(), zone.getLongitude()
            );
            if (distance <= zone.getRadiusKm()) {
                String severity = distance <= zone.getRadiusKm() * 0.5 ? "HIGH" : "MEDIUM";
                alerts.add(new Alert(
                        aircraft.getIcao24(),
                        aircraft.getCallsign(),
                        zone.getName(),
                        zone.getType(),
                        Math.round(distance * 100.0) / 100.0,
                        severity
                ));
            }
        }

        return alerts;
    }

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
