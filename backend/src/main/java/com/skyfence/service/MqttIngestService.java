package com.skyfence.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyfence.model.Aircraft;
import com.skyfence.util.IcaoCountry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fuente de datos alternativa a adsb.fi: sensores IoT que publican posiciones
 * de aeronaves por MQTT. Se activa con skyfence.mqtt.enabled=true y puede
 * convivir con el polling de la API (flightdata.api.enabled).
 *
 * Cada mensaje JSON del topic configurado se convierte al modelo Aircraft y
 * pasa por el mismo pipeline que la API: upsert + geofencing + alertas.
 * Formato esperado documentado en el README (sección "Integración MQTT").
 */
@Service
@ConditionalOnProperty(name = "skyfence.mqtt.enabled", havingValue = "true")
public class MqttIngestService implements MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttIngestService.class);
    private static final long RECONNECT_SECONDS = 30;

    private final String brokerUrl;
    private final String topic;
    private final String clientId;
    private final String username;
    private final String password;
    private final int qos;

    private final GeofenceService geofenceService;
    private final AlertService alertService;
    private final AircraftService aircraftService;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Counter processedCounter;
    private final Counter invalidCounter;
    private final ScheduledExecutorService reconnectExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "mqtt-reconnect");
                t.setDaemon(true);
                return t;
            });

    // Se escribe al (re)conectar y se lee desde los hilos de Paho (java:S3077)
    private final AtomicReference<MqttClient> client = new AtomicReference<>();

    public MqttIngestService(
            @Value("${skyfence.mqtt.broker-url}") String brokerUrl,
            @Value("${skyfence.mqtt.topic}") String topic,
            @Value("${skyfence.mqtt.client-id}") String clientId,
            @Value("${skyfence.mqtt.username:}") String username,
            @Value("${skyfence.mqtt.password:}") String password,
            @Value("${skyfence.mqtt.qos:1}") int qos,
            GeofenceService geofenceService,
            AlertService alertService,
            AircraftService aircraftService,
            MeterRegistry meterRegistry) {
        this.brokerUrl = brokerUrl;
        this.topic = topic;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
        this.qos = qos;
        this.geofenceService = geofenceService;
        this.alertService = alertService;
        this.aircraftService = aircraftService;
        this.processedCounter = Counter.builder("skyfence.mqtt.messages")
                .tag("result", "processed")
                .description("Mensajes MQTT de sensores procesados correctamente")
                .register(meterRegistry);
        this.invalidCounter = Counter.builder("skyfence.mqtt.messages")
                .tag("result", "invalid")
                .description("Mensajes MQTT descartados por formato o datos inválidos")
                .register(meterRegistry);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connect() {
        try {
            MqttClient newClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            if (!username.isBlank()) {
                options.setUserName(username);
                options.setPassword(password.toCharArray());
            }
            newClient.setCallback(this);
            client.set(newClient);
            newClient.connect(options);
            log.info("MQTT: conectado a {} (topic '{}')", brokerUrl, topic);
        } catch (MqttException e) {
            // La reconexión automática de Paho solo actúa tras una primera conexión
            // con éxito; si el broker aún no está arriba, reintentamos nosotros.
            log.warn("SECURITY ALERT: MQTT no disponible en {} ({}). Reintento en {}s",
                    brokerUrl, e.getMessage(), RECONNECT_SECONDS);
            reconnectExecutor.schedule(this::connect, RECONNECT_SECONDS, TimeUnit.SECONDS);
        }
    }

    /** Al (re)conectar se restaura la suscripción: con cleanSession no persiste. */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        try {
            client.get().subscribe(topic, qos);
            log.info("MQTT: suscrito a '{}' (qos {}){}", topic, qos, reconnect ? " tras reconexión" : "");
        } catch (MqttException e) {
            log.error("MQTT: no se pudo suscribir a '{}': {}", topic, e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("SECURITY ALERT: conexión MQTT perdida ({}). Paho reintentará automáticamente",
                cause != null ? cause.getMessage() : "sin causa");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // Nunca propagar excepciones: Paho cerraría la conexión.
        handleMessage(new String(message.getPayload(), StandardCharsets.UTF_8));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Solo consumimos; no publicamos.
    }

    /** Visible para tests: procesa un payload JSON de sensor. */
    void handleMessage(String payload) {
        MDC.put("requestId", "mqtt-" + UUID.randomUUID());
        try {
            SensorReading reading = mapper.readValue(payload, SensorReading.class);
            Aircraft aircraft = toAircraft(reading);
            if (aircraft == null) {
                invalidCounter.increment();
                return;
            }
            aircraftService.upsertAll(List.of(aircraft));
            geofenceService.checkAircraft(aircraft).forEach(alertService::sendAlert);
            processedCounter.increment();
            log.info("MQTT: posición de sensor procesada para {} ({}, {})",
                    aircraft.getIcao24(), aircraft.getLatitude(), aircraft.getLongitude());
        } catch (JsonProcessingException e) {
            invalidCounter.increment();
            log.warn("MQTT: mensaje descartado, JSON inválido ({} bytes)", payload.length());
        } catch (Exception e) {
            log.error("MQTT: error procesando mensaje", e);
        } finally {
            MDC.remove("requestId");
        }
    }

    /** Valida y convierte la lectura al modelo Aircraft; null si no es válida. */
    private Aircraft toAircraft(SensorReading r) {
        if (r.icao24() == null || !r.icao24().trim().matches("[A-Za-z0-9]{1,10}")) {
            log.warn("MQTT: mensaje descartado, icao24 ausente o inválido");
            return null;
        }
        if (r.latitude() == null || r.longitude() == null
                || Math.abs(r.latitude()) > 90 || Math.abs(r.longitude()) > 180) {
            log.warn("MQTT: mensaje de {} descartado, coordenadas inválidas", r.icao24().trim());
            return null;
        }
        String icao24 = r.icao24().trim();
        String callsign = r.callsign() == null || r.callsign().isBlank()
                ? "SENSOR"
                : r.callsign().trim().replaceAll("[^\\p{Print}]", "");
        if (callsign.length() > 12) callsign = callsign.substring(0, 12);
        boolean onGround = Boolean.TRUE.equals(r.onGround());
        return new Aircraft(icao24, callsign, IcaoCountry.fromHex(icao24),
                r.latitude(), r.longitude(), r.altitude(), r.velocity(), onGround);
    }

    @PreDestroy
    public void disconnect() {
        reconnectExecutor.shutdownNow();
        MqttClient current = client.get();
        if (current != null && current.isConnected()) {
            try {
                current.disconnect();
                log.info("MQTT: desconectado de {}", brokerUrl);
            } catch (MqttException e) {
                log.warn("MQTT: error al desconectar: {}", e.getMessage());
            }
        }
    }

    /**
     * Lectura de un sensor IoT. Altitud en metros, velocidad en m/s;
     * los campos opcionales pueden omitirse.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SensorReading(String icao24, String callsign, Double latitude, Double longitude,
                         Double altitude, Double velocity, Boolean onGround) {
    }
}
