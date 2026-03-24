package com.dronetrack.repository;

import com.dronetrack.model.RestrictedZone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestrictedZoneRepository extends JpaRepository<RestrictedZone, Long> {
}
