package com.skyfence.repository;

import com.skyfence.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    Page<Alert> findAllByOrderByDetectedAtDesc(Pageable pageable);
    Page<Alert> findBySeverityOrderByDetectedAtDesc(String severity, Pageable pageable);
    Page<Alert> findByDetectedAtAfterOrderByDetectedAtDesc(LocalDateTime after, Pageable pageable);
    Page<Alert> findBySeverityAndDetectedAtAfterOrderByDetectedAtDesc(String severity, LocalDateTime after, Pageable pageable);
}
