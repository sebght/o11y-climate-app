package com.formation.airquality.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.formation.airquality.model.AirQualityData;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class AirQualityService {
    private static final Logger logger = LoggerFactory.getLogger(AirQualityService.class);
    private final WebClient webClient;
    private final Counter apiCallCounter;
    private final Counter apiErrorCounter;
    private final Timer apiLatencyTimer;
    private final Random random = new Random();

    public AirQualityService(WebClient.Builder webClientBuilder, MeterRegistry meterRegistry) {
        this.webClient = webClientBuilder
                .baseUrl("https://api.openaq.org/v2")
                .build();

        // Métriques personnalisées
        this.apiCallCounter = Counter.builder("airquality.api.calls")
                .description("Nombre d'appels à l'API OpenAQ")
                .tag("service", "air-quality")
                .register(meterRegistry);

        this.apiErrorCounter = Counter.builder("airquality.api.errors")
                .description("Nombre d'erreurs API OpenAQ")
                .tag("service", "air-quality")
                .register(meterRegistry);

        this.apiLatencyTimer = Timer.builder("airquality.api.latency")
                .description("Latence des appels API OpenAQ")
                .tag("service", "air-quality")
                .register(meterRegistry);
    }

    public List<AirQualityData> getLatestMeasurements(String city, String country) {
        logger.info("Fetching air quality data for city: {}, country: {}", city, country);
        apiCallCounter.increment();

        return apiLatencyTimer.record(() -> {
            try {
                // Simulation de latence variable pour la formation
                simulateLatency();

                JsonNode response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/latest")
                                .queryParam("limit", "10")
                                .queryParam("page", "1")
                                .queryParam("city", city)
                                .queryParam("country", country)
                                .build())
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(10))
                        .block();

                if (response != null && response.has("results")) {
                    return parseResults(response.get("results"));
                }

                return new ArrayList<>();
            } catch (Exception e) {
                logger.error("Error fetching air quality data", e);
                apiErrorCounter.increment();
                throw new RuntimeException("Error fetching air quality data: " + e.getMessage());
            }
        });
    }

    public List<AirQualityData> getLatestByCoordinates(double latitude, double longitude, int radius) {
        logger.info("Fetching air quality data for coordinates: {}, {}, radius: {}", latitude, longitude, radius);
        apiCallCounter.increment();

        return apiLatencyTimer.record(() -> {
            try {
                simulateLatency();

                JsonNode response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/latest")
                                .queryParam("limit", "10")
                                .queryParam("coordinates", latitude + "," + longitude)
                                .queryParam("radius", radius)
                                .build())
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(10))
                        .block();

                if (response != null && response.has("results")) {
                    return parseResults(response.get("results"));
                }

                return new ArrayList<>();
            } catch (Exception e) {
                logger.error("Error fetching air quality data by coordinates", e);
                apiErrorCounter.increment();
                throw new RuntimeException("Error fetching air quality data: " + e.getMessage());
            }
        });
    }

    private List<AirQualityData> parseResults(JsonNode results) {
        List<AirQualityData> data = new ArrayList<>();

        for (JsonNode result : results) {
            JsonNode location = result.get("location");
            JsonNode coordinates = result.get("coordinates");
            JsonNode measurements = result.get("measurements");

            if (measurements != null && measurements.isArray()) {
                for (JsonNode measurement : measurements) {
                    AirQualityData aqData = new AirQualityData();
                    aqData.setCity(location != null ? location.asText() : "Unknown");
                    aqData.setCountry(result.has("country") ? result.get("country").asText() : "Unknown");

                    if (coordinates != null) {
                        aqData.setLatitude(coordinates.has("latitude") ? coordinates.get("latitude").asDouble() : 0.0);
                        aqData.setLongitude(coordinates.has("longitude") ? coordinates.get("longitude").asDouble() : 0.0);
                    }

                    aqData.setParameter(measurement.get("parameter").asText());
                    aqData.setValue(measurement.get("value").asDouble());
                    aqData.setUnit(measurement.get("unit").asText());
                    aqData.setLastUpdated(measurement.has("lastUpdated") ?
                            measurement.get("lastUpdated").asText() : "");

                    // Calcul simplifié de l'AQI
                    double value = aqData.getValue();
                    String parameter = aqData.getParameter();
                    aqData.setAqi(calculateAQI(parameter, value));
                    aqData.setQualityLevel(getQualityLevel(aqData.getAqi()));

                    data.add(aqData);
                }
            }
        }

        return data;
    }

    private int calculateAQI(String parameter, double value) {
        // Calcul simplifié de l'AQI pour la démonstration
        switch (parameter.toLowerCase()) {
            case "pm25":
                if (value <= 12) return 50;
                if (value <= 35.4) return 100;
                if (value <= 55.4) return 150;
                if (value <= 150.4) return 200;
                if (value <= 250.4) return 300;
                return 500;
            case "pm10":
                if (value <= 54) return 50;
                if (value <= 154) return 100;
                if (value <= 254) return 150;
                if (value <= 354) return 200;
                if (value <= 424) return 300;
                return 500;
            default:
                return (int) Math.min(value * 2, 500);
        }
    }

    private String getQualityLevel(int aqi) {
        if (aqi <= 50) return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }

    private void simulateLatency() {
        // Simulation de latence variable (100-500ms) pour rendre l'observabilité intéressante
        try {
            Thread.sleep(100 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
