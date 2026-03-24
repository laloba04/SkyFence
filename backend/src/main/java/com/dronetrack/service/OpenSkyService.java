package com.dronetrack.service;

import com.dronetrack.model.Aircraft;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OpenSkyService {

    private final WebClient webClient;
    private final WebClient authClient;
    private final GeofenceService geofenceService;
    private final AlertService alertService;

    private final String clientId;
    private final String clientSecret;
    private String accessToken = null;
    private Instant tokenExpiry = Instant.MIN;

    public OpenSkyService(
            @Value("${opensky.api.url}") String baseUrl,
            @Value("${opensky.api.client-id:}") String clientId,
            @Value("${opensky.api.client-secret:}") String clientSecret,
            GeofenceService geofenceService, 
            AlertService alertService) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.authClient = WebClient.builder().baseUrl("https://auth.opensky-network.org/auth/realms/opensky-network/protocol/openid-connect/token").build();
        this.geofenceService = geofenceService;
        this.alertService = alertService;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private synchronized String getValidToken() {
        if (clientId == null || clientId.isBlank()) return null; // Funciona en anónimo si no hay claves
        if (accessToken != null && Instant.now().isBefore(tokenExpiry)) return accessToken; // Usa token en caché

        try {
            Map<String, Object> response = authClient.post()
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                            .with("client_id", clientId)
                            .with("client_secret", clientSecret))
                    .retrieve().bodyToMono(Map.class).block();

            if (response != null && response.containsKey("access_token")) {
                this.accessToken = (String) response.get("access_token");
                this.tokenExpiry = Instant.now().plusSeconds(((Number) response.get("expires_in")).longValue() - 30);
                return accessToken;
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Scheduled(fixedDelayString = "${geofence.check.interval}")
    public void checkAndAlert() {
        fetchLiveAircraft().forEach(aircraft -> geofenceService.checkAircraft(aircraft).forEach(alertService::sendAlert));
    }

    @SuppressWarnings("unchecked")
    public List<Aircraft> fetchLiveAircraft() {
        try {
            String token = getValidToken();
            WebClient.RequestHeadersSpec<?> request = webClient.get()
                    .uri("/states/all?lamin=35.9&lamax=43.7&lomin=-9.3&lomax=4.3");
            
            if (token != null) {
                request = request.header("Authorization", "Bearer " + token);
            }

            Map<String, Object> response = request.retrieve()
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
