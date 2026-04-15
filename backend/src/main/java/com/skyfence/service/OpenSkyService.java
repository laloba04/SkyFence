package com.skyfence.service;

import com.skyfence.model.Aircraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OpenSkyService {

    private static final Logger log = LoggerFactory.getLogger(OpenSkyService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final WebClient webClient;
    private final GeofenceService geofenceService;
    private final AlertService alertService;

    public OpenSkyService(
            @Value("${flightdata.api.url}") String baseUrl,
            GeofenceService geofenceService,
            AlertService alertService) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
        this.geofenceService = geofenceService;
        this.alertService = alertService;
    }

    @Scheduled(fixedDelayString = "${geofence.check.interval}")
    public void checkAndAlert() {
        fetchLiveAircraft().forEach(aircraft ->
                geofenceService.checkAircraft(aircraft).forEach(alertService::sendAlert));
    }

    @SuppressWarnings("unchecked")
    public List<Aircraft> fetchLiveAircraft() {
        try {
            // Centro de España, radio 700 km cubre la peninsula + islas
            Map<String, Object> response = webClient.get()
                    .uri("/api/v2/lat/39.5/lon/-3.5/dist/700")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();

            List<Aircraft> list = new ArrayList<>();
            if (response == null || !response.containsKey("ac")) {
                log.warn("ADSB.fi: respuesta vacía o sin campo 'ac'");
                return list;
            }

            for (Map<String, Object> ac : (List<Map<String, Object>>) response.get("ac")) {
                try {
                    String icao24 = (String) ac.get("hex");
                    if (icao24 == null) continue;

                    String callsign = ac.get("flight") != null
                            ? ac.get("flight").toString().trim() : "N/A";
                    if (callsign.isEmpty()) callsign = "N/A";

                    Double lat = ac.get("lat") != null ? ((Number) ac.get("lat")).doubleValue() : null;
                    Double lon = ac.get("lon") != null ? ((Number) ac.get("lon")).doubleValue() : null;
                    if (lat == null || lon == null) continue;

                    // alt_baro puede ser número (pies) o el string "ground"
                    Object altBaro = ac.get("alt_baro");
                    Double altMeters = (altBaro instanceof Number)
                            ? ((Number) altBaro).doubleValue() * 0.3048 : null;

                    // gs = ground speed en nudos → m/s
                    Double velocityMs = ac.get("gs") != null
                            ? ((Number) ac.get("gs")).doubleValue() * 0.514444 : null;

                    boolean onGround = !(altBaro instanceof Number);

                    list.add(new Aircraft(icao24, callsign, "Unknown", lat, lon, altMeters, velocityMs, onGround));
                } catch (Exception ignored) {
                }
            }
            log.info("ADSB.fi: {} aeronaves obtenidas sobre España", list.size());
            return list;
        } catch (Exception e) {
            log.warn("Error al obtener aeronaves de ADSB.fi: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
