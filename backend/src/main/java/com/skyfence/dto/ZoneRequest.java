package com.skyfence.dto;

public class ZoneRequest {

    private String name;
    private String type;
    private Double latitude;
    private Double longitude;
    private Double radiusKm;

    public ZoneRequest() {}

    public String getName()           { return name; }
    public void setName(String name)  { this.name = name; }
    public String getType()           { return type; }
    public void setType(String type)  { this.type = type; }
    public Double getLatitude()                   { return latitude; }
    public void setLatitude(Double latitude)      { this.latitude = latitude; }
    public Double getLongitude()                  { return longitude; }
    public void setLongitude(Double longitude)    { this.longitude = longitude; }
    public Double getRadiusKm()                   { return radiusKm; }
    public void setRadiusKm(Double radiusKm)      { this.radiusKm = radiusKm; }
}
