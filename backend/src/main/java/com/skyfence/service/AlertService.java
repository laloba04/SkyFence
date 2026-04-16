package com.skyfence.service;

import com.skyfence.model.Alert;
import com.skyfence.repository.AlertRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AlertRepository alertRepository;

    public AlertService(SimpMessagingTemplate messagingTemplate, AlertRepository alertRepository) {
        this.messagingTemplate = messagingTemplate;
        this.alertRepository = alertRepository;
    }

    public void sendAlert(Alert alert) {
        alertRepository.save(alert);
        messagingTemplate.convertAndSend("/topic/alerts", alert);
    }
}
