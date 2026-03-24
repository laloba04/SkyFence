package com.dronetrack.model;

import jakarta.persistence.*;

@Entity
@Table(name = "aircraft")
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String icao24;
    private String callsign;
    private String originCountry;
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double velocity;
    private Boolean onGround;

    public Aircraft() {}

    public Aircraft(String icao24, String callsign, String originCountry, Double latitude, Double longitude,
                    Double altitude, Double velocity, Boolean onGround) {
        this.icao24 = icao24;
        this.callsign = callsign;
        this.originCountry = originCountry;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.velocity = velocity;
        this.onGround = onGround;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIcao24() { return icao24; }
    public void setIcao24(String icao24) { this.icao24 = icao24; }
    public String getCallsign() { return callsign; }
    public void setCallsign(String callsign) { this.callsign = callsign; }
    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getAltitude() { return altitude; }
    public void setAltitude(Double altitude) { this.altitude = altitude; }
    public Double getVelocity() { return velocity; }
    public void setVelocity(Double velocity) { this.velocity = velocity; }
    public Boolean getOnGround() { return onGround; }
    public void setOnGround(Boolean onGround) { this.onGround = onGround; }
}
