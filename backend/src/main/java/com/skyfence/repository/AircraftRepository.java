package com.skyfence.repository;

import com.skyfence.model.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AircraftRepository extends JpaRepository<Aircraft, Long> {
    List<Aircraft> findByOnGroundFalse();
    Optional<Aircraft> findByIcao24(String icao24);
    List<Aircraft> findByIcao24In(Collection<String> icao24s);
    @Modifying
    @Query("delete from Aircraft a where a.lastSeen < :cutoff")
    int deleteByLastSeenBefore(@Param("cutoff") LocalDateTime cutoff);
}
