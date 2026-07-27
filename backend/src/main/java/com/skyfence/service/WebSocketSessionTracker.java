package com.skyfence.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cuenta las sesiones STOMP activas (issue #48). Permite ver en Grafana
 * cuántos clientes están recibiendo alertas en tiempo real y alertar si el
 * WebSocket se queda sin nadie conectado durante mucho rato.
 */
@Component
public class WebSocketSessionTracker {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionTracker.class);

    private final AtomicInteger activeSessions = new AtomicInteger();

    public WebSocketSessionTracker(MeterRegistry meterRegistry) {
        Gauge.builder("skyfence.websocket.sessions", activeSessions, AtomicInteger::get)
                .description("Sesiones WebSocket (STOMP) actualmente conectadas")
                .register(meterRegistry);
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        log.info("WebSocket: cliente conectado ({} sesiones activas)", activeSessions.incrementAndGet());
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        // updateAndGet evita que el contador baje de 0 si llega un disconnect
        // huérfano (p. ej. una sesión que nunca llegó a completar el CONNECT)
        int remaining = activeSessions.updateAndGet(n -> n > 0 ? n - 1 : 0);
        log.info("WebSocket: cliente desconectado ({} sesiones activas)", remaining);
    }

    /** Visible para tests. */
    int getActiveSessions() {
        return activeSessions.get();
    }
}
