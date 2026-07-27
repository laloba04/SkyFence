package com.skyfence.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebSocketSessionTrackerTest {

    SimpleMeterRegistry meterRegistry;
    WebSocketSessionTracker tracker;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        tracker = new WebSocketSessionTracker(meterRegistry);
    }

    private void connect() {
        Message<byte[]> message = new GenericMessage<>(new byte[0]);
        tracker.onConnected(new SessionConnectedEvent(this, message));
    }

    private void disconnect(String sessionId) {
        Message<byte[]> message = new GenericMessage<>(new byte[0]);
        tracker.onDisconnected(new SessionDisconnectEvent(this, message, sessionId, null));
    }

    private double gauge() {
        return meterRegistry.get("skyfence.websocket.sessions").gauge().value();
    }

    @Test
    void countsConnectionsAndDisconnections() {
        connect();
        connect();
        connect();
        assertEquals(3, tracker.getActiveSessions());
        assertEquals(3.0, gauge());

        disconnect("s1");
        assertEquals(2, tracker.getActiveSessions());
        assertEquals(2.0, gauge());
    }

    @Test
    void startsAtZero() {
        assertEquals(0, tracker.getActiveSessions());
        assertEquals(0.0, gauge());
    }

    @Test
    void neverGoesNegativeOnOrphanDisconnect() {
        disconnect("huerfana");
        disconnect("otra");

        assertEquals(0, tracker.getActiveSessions());
        assertEquals(0.0, gauge());
    }
}
