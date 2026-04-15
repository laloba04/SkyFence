package com.skyfence.service;

import com.skyfence.model.Aircraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class OpenSkyService {

    private static final Logger log = LoggerFactory.getLogger(OpenSkyService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final WebClient webClient;
    private final GeofenceService geofenceService;
    private final AlertService alertService;

    private final String username;
    private final String password;

    public OpenSkyService(
            @Value("${opensky.api.url}") String baseUrl,
            @Value("${opensky.api.username:}") String username,
            @Value("${opensky.api.password:}") String password,
            GeofenceService geofenceService,
            AlertService alertService) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.geofenceService = geofenceService;
        this.alertService = alertService;
        this.username = username;
        this.password = password;
    }

    @Scheduled(fixedDelayString = "${geofence.check.interval}")
    public void checkAndAlert() {
        fetchLiveAircraft().forEach(aircraft -> geofenceService.checkAircraft(aircraft).forEach(alertService::sendAlert));
    }

    @SuppressWarnings("unchecked")
    public List<Aircraft> fetchLiveAircraft() {
        try {
            WebClient.RequestHeadersSpec<?> request = webClient.get()
                    .uri("/states/all?lamin=35.9&lamax=43.7&lomin=-9.3&lomax=4.3");

            if (username != null && !username.isBlank()) {
                String credentials = Base64.getEncoder()
                        .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
                request = request.header("Authorization", "Basic " + credentials);
                log.debug("Usando Basic Auth con usuario: {}", username);
            } else {
                log.debug("Usando acceso anónimo a OpenSky");
            }

            Map<String, Object> response = request.retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
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
            log.info("OpenSky: {} aeronaves obtenidas", list.size());
            return list;
        } catch (Exception e) {
            log.warn("Error al obtener aeronaves de OpenSky: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
