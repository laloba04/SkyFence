package com.skyfence.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aircraftIcao;
    private String aircraftCallsign;
    private String zoneName;
    private String zoneType;
    private Double distanceKm;
    private String severity;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    public Alert() {}

    public Alert(String aircraftIcao, String aircraftCallsign, String zoneName, String zoneType,
                 Double distanceKm, String severity) {
        this.aircraftIcao = aircraftIcao;
        this.aircraftCallsign = aircraftCallsign;
        this.zoneName = zoneName;
        this.zoneType = zoneType;
        this.distanceKm = distanceKm;
        this.severity = severity;
        this.detectedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAircraftIcao() { return aircraftIcao; }
    public void setAircraftIcao(String aircraftIcao) { this.aircraftIcao = aircraftIcao; }
    public String getAircraftCallsign() { return aircraftCallsign; }
    public void setAircraftCallsign(String aircraftCallsign) { this.aircraftCallsign = aircraftCallsign; }
    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public String getZoneType() { return zoneType; }
    public void setZoneType(String zoneType) { this.zoneType = zoneType; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
