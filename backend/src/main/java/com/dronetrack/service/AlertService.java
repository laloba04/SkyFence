package com.dronetrack.service;

import com.dronetrack.model.Alert;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private final SimpMessagingTemplate messagingTemplate;

    public AlertService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendAlert(Alert alert) {
        messagingTemplate.convertAndSend("/topic/alerts", alert);
    }
}
