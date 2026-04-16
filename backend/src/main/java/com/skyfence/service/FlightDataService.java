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
public class FlightDataService {

    private static final Logger log = LoggerFactory.getLogger(FlightDataService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final long CACHE_TTL_MS = 60_000; // 1 minuto entre llamadas a adsb.fi

    private final WebClient webClient;
    private final GeofenceService geofenceService;
    private final AlertService alertService;
    private final AircraftService aircraftService;

    private volatile List<Aircraft> cachedAircraft = new ArrayList<>();
    private volatile long lastFetchTime = 0;

    public FlightDataService(
            @Value("${flightdata.api.url}") String baseUrl,
            GeofenceService geofenceService,
            AlertService alertService,
            AircraftService aircraftService) {
        this.aircraftService = aircraftService;
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
        long now = System.currentTimeMillis();
        if (now - lastFetchTime < CACHE_TTL_MS && !cachedAircraft.isEmpty()) {
            return cachedAircraft;
        }

        try {
            Map<String, Object> response = webClient.get()
                    .uri("/api/v2/lat/39.5/lon/-3.5/dist/250")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();

            List<Aircraft> list = new ArrayList<>();
            if (response == null || !response.containsKey("aircraft")) {
                log.warn("ADSB.fi: respuesta vacía o sin campo 'aircraft'");
                return cachedAircraft;
            }

            for (Map<String, Object> ac : (List<Map<String, Object>>) response.get("aircraft")) {
                try {
                    String icao24 = (String) ac.get("hex");
                    if (icao24 == null) continue;

                    String callsign = ac.get("flight") != null
                            ? ac.get("flight").toString().trim() : "N/A";
                    if (callsign.isEmpty()) callsign = "N/A";

                    Double lat = ac.get("lat") != null ? ((Number) ac.get("lat")).doubleValue() : null;
                    Double lon = ac.get("lon") != null ? ((Number) ac.get("lon")).doubleValue() : null;
                    if (lat == null || lon == null) continue;

                    Object altBaro = ac.get("alt_baro");
                    Double altMeters = (altBaro instanceof Number)
                            ? ((Number) altBaro).doubleValue() * 0.3048 : null;

                    Double velocityMs = ac.get("gs") != null
                            ? ((Number) ac.get("gs")).doubleValue() * 0.514444 : null;

                    boolean onGround = !(altBaro instanceof Number);

                    list.add(new Aircraft(icao24, callsign, countryFromIcao(icao24), lat, lon, altMeters, velocityMs, onGround));
                } catch (Exception ignored) {
                }
            }
            log.info("ADSB.fi: {} aeronaves obtenidas sobre España", list.size());
            aircraftService.upsertAll(list);
            cachedAircraft = list;
            lastFetchTime = now;
            return list;
        } catch (Exception e) {
            log.warn("Error al obtener aeronaves de ADSB.fi: {}", e.getMessage());
            return cachedAircraft;
        }
    }

    private static String countryFromIcao(String hex) {
        if (hex == null || hex.isEmpty()) return "Unknown";
        try {
            int code = Integer.parseInt(hex.trim(), 16);
            // Europa
            if (code >= 0x300000 && code <= 0x33FFFF) return "Italy";
            if (code >= 0x340000 && code <= 0x37FFFF) return "Spain";
            if (code >= 0x380000 && code <= 0x3BFFFF) return "France";
            if (code >= 0x3C0000 && code <= 0x3FFFFF) return "Germany";
            if (code >= 0x400000 && code <= 0x43FFFF) return "United Kingdom";
            if (code >= 0x440000 && code <= 0x447FFF) return "Austria";
            if (code >= 0x448000 && code <= 0x44FFFF) return "Belgium";
            if (code >= 0x450000 && code <= 0x457FFF) return "Bulgaria";
            if (code >= 0x458000 && code <= 0x45FFFF) return "Denmark";
            if (code >= 0x460000 && code <= 0x467FFF) return "Finland";
            if (code >= 0x468000 && code <= 0x46FFFF) return "Greece";
            if (code >= 0x470000 && code <= 0x477FFF) return "Hungary";
            if (code >= 0x478000 && code <= 0x47FFFF) return "Croatia";
            if (code >= 0x480000 && code <= 0x487FFF) return "Netherlands";
            if (code >= 0x488000 && code <= 0x48FFFF) return "Poland";
            if (code >= 0x490000 && code <= 0x497FFF) return "Portugal";
            if (code >= 0x498000 && code <= 0x49FFFF) return "Czech Republic";
            if (code >= 0x4A0000 && code <= 0x4A7FFF) return "Romania";
            if (code >= 0x4A8000 && code <= 0x4AFFFF) return "Sweden";
            if (code >= 0x4B0000 && code <= 0x4B7FFF) return "Switzerland";
            if (code >= 0x4B8000 && code <= 0x4BFFFF) return "Turkey";
            if (code >= 0x4C0000 && code <= 0x4C7FFF) return "Norway";
            if (code >= 0x4CA000 && code <= 0x4CAFFF) return "Ireland";
            if (code >= 0x4D0000 && code <= 0x4D7FFF) return "Slovakia";
            if (code >= 0x4D8000 && code <= 0x4DFFFF) return "Slovenia";
            if (code >= 0x4E0000 && code <= 0x4E7FFF) return "Serbia";
            if (code >= 0x4E8000 && code <= 0x4EFFFF) return "Ukraine";
            // Rusia y exURSS
            if (code >= 0x100000 && code <= 0x1FFFFF) return "Russia";
            // Oriente Medio
            if (code >= 0x710000 && code <= 0x717FFF) return "Saudi Arabia";
            if (code >= 0x730000 && code <= 0x737FFF) return "UAE";
            if (code >= 0x738000 && code <= 0x73FFFF) return "Israel";
            if (code >= 0x740000 && code <= 0x747FFF) return "Qatar";
            // Asia-Pacífico
            if (code >= 0x780000 && code <= 0x7BFFFF) return "China";
            if (code >= 0x7C0000 && code <= 0x7FFFFF) return "Australia";
            if (code >= 0x800000 && code <= 0x83FFFF) return "India";
            if (code >= 0x840000 && code <= 0x87FFFF) return "Japan";
            if (code >= 0x880000 && code <= 0x887FFF) return "South Korea";
            // América
            if (code >= 0xA00000 && code <= 0xAFFFFF) return "United States";
            if (code >= 0xC00000 && code <= 0xC3FFFF) return "Canada";
            if (code >= 0x0D0000 && code <= 0x0FFFFF) return "Mexico";
            if (code >= 0xE40000 && code <= 0xE7FFFF) return "Brazil";
            // África
            if (code >= 0x008000 && code <= 0x00FFFF) return "South Africa";
            if (code >= 0x010000 && code <= 0x017FFF) return "Egypt";
        } catch (NumberFormatException ignored) {
        }
        return "Unknown";
    }
}
