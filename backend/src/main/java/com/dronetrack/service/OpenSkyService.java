package com.dronetrack.service;

import com.dronetrack.model.Aircraft;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OpenSkyService {

    private final WebClient webClient;
    private final GeofenceService geofenceService;
    private final AlertService alertService;

    public OpenSkyService(@Value("${opensky.api.url}") String baseUrl, GeofenceService geofenceService, AlertService alertService) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.geofenceService = geofenceService;
        this.alertService = alertService;
    }

    @Scheduled(fixedDelayString = "${geofence.check.interval}")
    public void checkAndAlert() {
        fetchLiveAircraft().forEach(aircraft -> geofenceService.checkAircraft(aircraft).forEach(alertService::sendAlert));
    }

    @SuppressWarnings("unchecked")
    public List<Aircraft> fetchLiveAircraft() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/states/all?lamin=35.9&lamax=43.7&lomin=-9.3&lomax=4.3")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Aircraft> list = new ArrayList<>();
            if (response == null || !response.containsKey("states")) {
                return list;
            }

            for (List<Object> s : (List<List<Object>>) response.get("states")) {
                try {
                    list.add(new Aircraft(
                            (String) s.get(0),
                            s.get(1) != null ? s.get(1).toString().trim() : "N/A",
                            (String) s.get(2),
                            s.get(6) != null ? ((Number) s.get(6)).doubleValue() : null,
                            s.get(5) != null ? ((Number) s.get(5)).doubleValue() : null,
                            s.get(7) != null ? ((Number) s.get(7)).doubleValue() : null,
                            s.get(9) != null ? ((Number) s.get(9)).doubleValue() : null,
                            (Boolean) s.get(8)
                    ));
                } catch (Exception ignored) {
                }
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
