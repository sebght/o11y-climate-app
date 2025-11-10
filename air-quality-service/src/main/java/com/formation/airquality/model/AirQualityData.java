package com.formation.airquality.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AirQualityData {
    private String city;
    private String country;
    private double latitude;
    private double longitude;
    private String parameter;
    private double value;
    private String unit;
    private String lastUpdated;
    private int aqi;
    private String qualityLevel;
}
