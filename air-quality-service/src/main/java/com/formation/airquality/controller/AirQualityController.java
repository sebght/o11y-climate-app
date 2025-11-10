package com.formation.airquality.controller;

import com.formation.airquality.model.AirQualityData;
import com.formation.airquality.service.AirQualityService;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/air-quality")
@CrossOrigin(origins = "*")
public class AirQualityController {
    private static final Logger logger = LoggerFactory.getLogger(AirQualityController.class);
    private final AirQualityService airQualityService;

    public AirQualityController(AirQualityService airQualityService) {
        this.airQualityService = airQualityService;
    }

    @GetMapping("/city")
    @Timed(value = "airquality.city.request", description = "Time taken to get air quality by city")
    public ResponseEntity<List<AirQualityData>> getAirQualityByCity(
            @RequestParam String city,
            @RequestParam(required = false, defaultValue = "FR") String country) {
        logger.info("GET /api/air-quality/city?city={}&country={}", city, country);
        List<AirQualityData> data = airQualityService.getLatestMeasurements(city, country);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/coordinates")
    @Timed(value = "airquality.coordinates.request", description = "Time taken to get air quality by coordinates")
    public ResponseEntity<List<AirQualityData>> getAirQualityByCoordinates(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false, defaultValue = "25000") int radius) {
        logger.info("GET /api/air-quality/coordinates?lat={}&lon={}&radius={}",
                latitude, longitude, radius);
        List<AirQualityData> data = airQualityService.getLatestByCoordinates(latitude, longitude, radius);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
