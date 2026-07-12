package com.skyfence.repository;

import com.skyfence.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    Page<Alert> findAllByOrderByDetectedAtDesc(Pageable pageable);
    Page<Alert> findBySeverityOrderByDetectedAtDesc(String severity, Pageable pageable);
    Page<Alert> findByDetectedAtAfterOrderByDetectedAtDesc(LocalDateTime after, Pageable pageable);
    Page<Alert> findBySeverityAndDetectedAtAfterOrderByDetectedAtDesc(String severity, LocalDateTime after, Pageable pageable);
    // DELETE masivo en una sola sentencia: el delete derivado cargaría cada
    // entidad en memoria (cientos de miles de filas) antes de borrarla
    @Modifying
    @Query("delete from Alert a where a.detectedAt < :cutoff")
    int deleteByDetectedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
