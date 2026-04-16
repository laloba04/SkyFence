package com.skyfence.controller;

import com.skyfence.model.Alert;
import com.skyfence.repository.AlertRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts", description = "Histórico de alertas de intrusión")
public class AlertController {

    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping
    @Operation(summary = "Histórico de alertas paginado con filtros opcionales")
    public Page<Alert> getAlerts(
            @Parameter(description = "Número de página (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filtrar por severidad: HIGH o MEDIUM") @RequestParam(required = false) String severity,
            @Parameter(description = "Filtrar alertas de las últimas N horas") @RequestParam(required = false) Integer hours) {

        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        LocalDateTime after = hours != null ? LocalDateTime.now().minusHours(hours) : null;

        if (severity != null && after != null) {
            return alertRepository.findBySeverityAndDetectedAtAfterOrderByDetectedAtDesc(severity.toUpperCase(), after, pageable);
        } else if (severity != null) {
            return alertRepository.findBySeverityOrderByDetectedAtDesc(severity.toUpperCase(), pageable);
        } else if (after != null) {
            return alertRepository.findByDetectedAtAfterOrderByDetectedAtDesc(after, pageable);
        } else {
            return alertRepository.findAllByOrderByDetectedAtDesc(pageable);
        }
    }
}
