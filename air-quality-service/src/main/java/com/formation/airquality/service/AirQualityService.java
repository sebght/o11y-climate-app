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

    private final String apiKey;

    // Coordonnées des villes françaises pour la recherche (latitude, longitude)
    private static final java.util.Map<String, double[]> CITY_COORDINATES = new java.util.HashMap<>();
    static {
        // Grandes villes
        CITY_COORDINATES.put("Paris", new double[]{48.8566, 2.3522});
        CITY_COORDINATES.put("Lyon", new double[]{45.7640, 4.8357});
        CITY_COORDINATES.put("Marseille", new double[]{43.2965, 5.3698});
        CITY_COORDINATES.put("Toulouse", new double[]{43.6047, 1.4442});
        CITY_COORDINATES.put("Nice", new double[]{43.7102, 7.2620});
        CITY_COORDINATES.put("Nantes", new double[]{47.2184, -1.5536});
        CITY_COORDINATES.put("Strasbourg", new double[]{48.5734, 7.7521});
        CITY_COORDINATES.put("Bordeaux", new double[]{44.8378, -0.5792});
        CITY_COORDINATES.put("Lille", new double[]{50.6292, 3.0573});
        CITY_COORDINATES.put("Rennes", new double[]{48.1173, -1.6778});
        CITY_COORDINATES.put("Montpellier", new double[]{43.6108, 3.8767});
        CITY_COORDINATES.put("Grenoble", new double[]{45.1885, 5.7245});

        // Villes moyennes et régionales
        CITY_COORDINATES.put("Reims", new double[]{49.2583, 4.0317});
        CITY_COORDINATES.put("Le Havre", new double[]{49.4944, 0.1079});
        CITY_COORDINATES.put("Saint-Étienne", new double[]{45.4397, 4.3872});
        CITY_COORDINATES.put("Toulon", new double[]{43.1242, 5.9280});
        CITY_COORDINATES.put("Angers", new double[]{47.4784, -0.5632});
        CITY_COORDINATES.put("Dijon", new double[]{47.3220, 5.0415});
        CITY_COORDINATES.put("Brest", new double[]{48.3905, -4.4861});
        CITY_COORDINATES.put("Le Mans", new double[]{48.0077, 0.1984});
        CITY_COORDINATES.put("Clermont-Ferrand", new double[]{45.7772, 3.0870});
        CITY_COORDINATES.put("Amiens", new double[]{49.8941, 2.2958});
        CITY_COORDINATES.put("Aix-en-Provence", new double[]{43.5297, 5.4474});
        CITY_COORDINATES.put("Limoges", new double[]{45.8336, 1.2611});
        CITY_COORDINATES.put("Tours", new double[]{47.3941, 0.6848});
        CITY_COORDINATES.put("Orléans", new double[]{47.9029, 1.9093});
        CITY_COORDINATES.put("Metz", new double[]{49.1193, 6.1757});
        CITY_COORDINATES.put("Besançon", new double[]{47.2380, 6.0243});
        CITY_COORDINATES.put("Perpignan", new double[]{42.6886, 2.8948});
        CITY_COORDINATES.put("Caen", new double[]{49.1829, -0.3707});
        CITY_COORDINATES.put("Rouen", new double[]{49.4432, 1.0993});
        CITY_COORDINATES.put("Nancy", new double[]{48.6921, 6.1844});
        CITY_COORDINATES.put("Argenteuil", new double[]{48.9474, 2.2464});
        CITY_COORDINATES.put("Montreuil", new double[]{48.8634, 2.4428});
        CITY_COORDINATES.put("Mulhouse", new double[]{47.7508, 7.3359});
        CITY_COORDINATES.put("Pau", new double[]{43.2951, -0.3708});
        CITY_COORDINATES.put("Avignon", new double[]{43.9493, 4.8055});
    }

    public AirQualityService(WebClient.Builder webClientBuilder,
                            MeterRegistry meterRegistry,
                            @org.springframework.beans.factory.annotation.Value("${openaq.api.key:}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = webClientBuilder
                .baseUrl("https://api.openaq.org/v3")
                .defaultHeader("X-API-Key", apiKey)
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

                // Récupérer les coordonnées de la ville
                double[] coords = CITY_COORDINATES.getOrDefault(city, CITY_COORDINATES.get("Paris"));
                double latitude = coords[0];
                double longitude = coords[1];

                // Appel à l'API OpenAQ v3 avec les coordonnées de la ville (rayon max 25km)
                JsonNode response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/locations")
                                .queryParam("coordinates", latitude + "," + longitude)
                                .queryParam("radius", "25000")
                                .queryParam("limit", "20")
                                .build())
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(10))
                        .block();

                if (response != null && response.has("results")) {
                    JsonNode results = response.get("results");
                    List<AirQualityData> data = parseV3Results(results, city, country);

                    if (data.isEmpty()) {
                        logger.warn("No air quality data found for city: {}", city);
                    }

                    return data;
                }

                logger.warn("Empty response from OpenAQ API");
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

                // L'API OpenAQ limite le rayon à 25000 mètres maximum
                int actualRadius = Math.min(radius, 25000);

                // Appel à l'API OpenAQ v3 avec coordonnées et rayon
                JsonNode response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/locations")
                                .queryParam("coordinates", latitude + "," + longitude)
                                .queryParam("radius", String.valueOf(actualRadius))
                                .queryParam("limit", "20")
                                .build())
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(10))
                        .block();

                if (response != null && response.has("results")) {
                    JsonNode results = response.get("results");
                    List<AirQualityData> data = parseV3Results(results, "Unknown", "FR");

                    if (data.isEmpty()) {
                        logger.warn("No air quality data found for coordinates: {}, {}", latitude, longitude);
                    }

                    return data;
                }

                logger.warn("Empty response from OpenAQ API");
                return new ArrayList<>();
            } catch (Exception e) {
                logger.error("Error fetching air quality data by coordinates", e);
                apiErrorCounter.increment();
                throw new RuntimeException("Error fetching air quality data: " + e.getMessage());
            }
        });
    }

    private List<AirQualityData> parseLocationsWithLatest(JsonNode locations, String cityName, String countryCode) {
        List<AirQualityData> data = new ArrayList<>();
        java.util.Set<String> seenParameters = new java.util.HashSet<>();

        for (JsonNode location : locations) {
            try {
                // Informations de la location
                String locationName = location.has("name") ? location.get("name").asText() : cityName;
                JsonNode coordinates = location.get("coordinates");
                double latitude = coordinates != null && coordinates.has("latitude") ? coordinates.get("latitude").asDouble() : 0.0;
                double longitude = coordinates != null && coordinates.has("longitude") ? coordinates.get("longitude").asDouble() : 0.0;

                JsonNode country = location.get("country");
                String countryName = country != null && country.has("name") ? country.get("name").asText() : countryCode;

                // Récupérer les paramètres mesurés sur cette location
                JsonNode parameters = location.get("parameters");
                if (parameters != null && parameters.isArray()) {
                    for (JsonNode param : parameters) {
                        String paramId = param.has("id") ? String.valueOf(param.get("id").asInt()) : null;
                        String paramName = param.has("name") ? param.get("name").asText() : null;
                        String paramDisplayName = param.has("displayName") ? param.get("displayName").asText() : null;

                        // Choisir le meilleur nom disponible
                        String finalParamName = (paramDisplayName != null && !paramDisplayName.isEmpty()) ? paramDisplayName :
                                                (paramName != null && !paramName.isEmpty()) ? paramName : paramId;

                        if (finalParamName == null || seenParameters.contains(finalParamName)) continue;
                        seenParameters.add(finalParamName);

                        AirQualityData aqData = new AirQualityData();
                        aqData.setCity(locationName);
                        aqData.setCountry(countryName);
                        aqData.setLatitude(latitude);
                        aqData.setLongitude(longitude);
                        aqData.setParameter(finalParamName);

                        // Récupérer la dernière valeur mesurée
                        double lastValue = param.has("lastValue") ? param.get("lastValue").asDouble() : 0.0;
                        aqData.setValue(lastValue);

                        // Unité
                        String unit = param.has("units") ? param.get("units").asText() : "";
                        aqData.setUnit(unit);

                        // Date de dernière mise à jour
                        if (param.has("lastUpdated")) {
                            aqData.setLastUpdated(param.get("lastUpdated").asText());
                        }

                        // Calcul AQI
                        aqData.setAqi(calculateAQI(finalParamName, lastValue));
                        aqData.setQualityLevel(getQualityLevel(aqData.getAqi()));

                        data.add(aqData);

                        // Limiter à 10 paramètres
                        if (data.size() >= 10) return data;
                    }
                }
            } catch (Exception e) {
                logger.warn("Error parsing location: {}", e.getMessage());
            }
        }

        return data;
    }

    private List<AirQualityData> parseMeasurements(JsonNode measurements, String cityName, String countryCode, double targetLat, double targetLng) {
        List<AirQualityData> data = new ArrayList<>();
        java.util.Set<String> seenParameters = new java.util.HashSet<>();

        for (JsonNode measurement : measurements) {
            try {
                // Extraire les informations de la mesure
                JsonNode parameter = measurement.get("parameter");
                if (parameter == null) continue;

                String paramId = parameter.has("id") ? String.valueOf(parameter.get("id").asInt()) : null;
                String paramName = parameter.has("name") ? parameter.get("name").asText() : null;

                // Utiliser le nom si disponible, sinon l'ID
                String finalParamName = (paramName != null && !paramName.isEmpty()) ? paramName : paramId;
                if (finalParamName == null || finalParamName.equals("null")) continue;

                // Éviter les doublons de paramètres
                if (seenParameters.contains(finalParamName)) continue;
                seenParameters.add(finalParamName);

                AirQualityData aqData = new AirQualityData();

                // Informations de localisation
                JsonNode coordinates = measurement.get("coordinates");
                if (coordinates != null) {
                    aqData.setLatitude(coordinates.has("latitude") ? coordinates.get("latitude").asDouble() : targetLat);
                    aqData.setLongitude(coordinates.has("longitude") ? coordinates.get("longitude").asDouble() : targetLng);
                }

                JsonNode location = measurement.get("location");
                if (location != null && location.has("name")) {
                    aqData.setCity(location.get("name").asText());
                } else {
                    aqData.setCity(cityName);
                }

                JsonNode country = measurement.get("country");
                if (country != null && country.has("name")) {
                    aqData.setCountry(country.get("name").asText());
                } else {
                    aqData.setCountry(countryCode);
                }

                // Paramètre et valeur
                aqData.setParameter(finalParamName);
                aqData.setValue(measurement.has("value") ? measurement.get("value").asDouble() : 0.0);

                // Unité
                String unit = parameter.has("units") ? parameter.get("units").asText() : "";
                aqData.setUnit(unit);

                // Date
                JsonNode period = measurement.get("period");
                if (period != null && period.has("datetimeTo")) {
                    JsonNode datetimeTo = period.get("datetimeTo");
                    if (datetimeTo.has("utc")) {
                        aqData.setLastUpdated(datetimeTo.get("utc").asText());
                    }
                }

                // Calcul AQI
                double value = aqData.getValue();
                aqData.setAqi(calculateAQI(finalParamName, value));
                aqData.setQualityLevel(getQualityLevel(aqData.getAqi()));

                data.add(aqData);

                // Limiter à 10 paramètres différents
                if (data.size() >= 10) break;

            } catch (Exception e) {
                logger.warn("Error parsing measurement: {}", e.getMessage());
            }
        }

        return data;
    }

    private List<AirQualityData> parseV3Results(JsonNode results, String cityName, String countryCode) {
        List<AirQualityData> data = new ArrayList<>();

        for (JsonNode location : results) {
            // Extraire les informations de localisation
            String locationName = location.has("name") ? location.get("name").asText() : cityName;
            JsonNode coordinates = location.get("coordinates");
            JsonNode country = location.get("country");

            String countryName = country != null && country.has("name") ?
                country.get("name").asText() : countryCode;

            double latitude = coordinates != null && coordinates.has("latitude") ?
                coordinates.get("latitude").asDouble() : 0.0;
            double longitude = coordinates != null && coordinates.has("longitude") ?
                coordinates.get("longitude").asDouble() : 0.0;

            // Construire un mapping sensorId -> paramètre depuis le tableau sensors
            java.util.Map<Integer, JsonNode> sensorParameterMap = new java.util.HashMap<>();
            if (location.has("sensors") && location.get("sensors").isArray()) {
                for (JsonNode sensor : location.get("sensors")) {
                    int sensorId = sensor.has("id") ? sensor.get("id").asInt() : 0;
                    if (sensorId > 0 && sensor.has("parameter")) {
                        sensorParameterMap.put(sensorId, sensor.get("parameter"));
                    }
                }
            }

            // Récupérer les données latest de cette location
            int locationId = location.has("id") ? location.get("id").asInt() : 0;
            if (locationId > 0) {
                try {
                    JsonNode latestData = webClient.get()
                            .uri("/locations/" + locationId + "/latest")
                            .retrieve()
                            .bodyToMono(JsonNode.class)
                            .timeout(Duration.ofSeconds(5))
                            .block();

                    if (latestData != null && latestData.has("results")) {
                        for (JsonNode measurement : latestData.get("results")) {
                            AirQualityData aqData = new AirQualityData();
                            aqData.setCity(locationName);
                            aqData.setCountry(countryName);
                            aqData.setLatitude(latitude);
                            aqData.setLongitude(longitude);

                            // Récupérer le sensor ID depuis le measurement
                            int sensorId = measurement.has("sensorsId") ? measurement.get("sensorsId").asInt() : 0;

                            // Chercher le paramètre correspondant dans le mapping
                            String paramName = "unknown";
                            String unit = "";

                            if (sensorId > 0 && sensorParameterMap.containsKey(sensorId)) {
                                JsonNode parameter = sensorParameterMap.get(sensorId);

                                // Essayer d'abord le champ "name"
                                if (parameter.has("name") && !parameter.get("name").isNull()) {
                                    paramName = parameter.get("name").asText();
                                }
                                // Essayer le champ "displayName" si disponible
                                else if (parameter.has("displayName") && !parameter.get("displayName").isNull()) {
                                    paramName = parameter.get("displayName").asText();
                                }
                                // Essayer le champ "id" en dernier recours
                                else if (parameter.has("id") && !parameter.get("id").isNull()) {
                                    paramName = String.valueOf(parameter.get("id").asInt());
                                }

                                // Récupérer l'unité
                                if (parameter.has("units") && !parameter.get("units").isNull()) {
                                    unit = parameter.get("units").asText();
                                }
                            }

                            aqData.setParameter(paramName);
                            aqData.setValue(measurement.has("value") ? measurement.get("value").asDouble() : 0.0);
                            aqData.setUnit(unit);

                            JsonNode datetime = measurement.get("datetime");
                            if (datetime != null && datetime.has("utc")) {
                                aqData.setLastUpdated(datetime.get("utc").asText());
                            }

                            // Calcul de l'AQI - paramName ne peut jamais être null ici
                            double value = aqData.getValue();
                            aqData.setAqi(calculateAQI(paramName, value));
                            aqData.setQualityLevel(getQualityLevel(aqData.getAqi()));

                            data.add(aqData);

                            // Limiter à 10 mesures max
                            if (data.size() >= 10) {
                                return data;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error fetching latest data for location {}: {}", locationId, e.getMessage());
                }
            }
        }

        return data;
    }

    private int calculateAQI(String parameter, double value) {
        // Calcul simplifié de l'AQI pour la démonstration
        if (parameter == null) {
            return (int) Math.min(value * 2, 500);
        }

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
        if (aqi <= 50) return "Bon";
        if (aqi <= 100) return "Moyen";
        if (aqi <= 150) return "Dégradé";
        if (aqi <= 200) return "Mauvais";
        if (aqi <= 300) return "Très mauvais";
        return "Extrême";
    }

    private void simulateLatency() {
        // Simulation de latence variable (100-1500ms) pour rendre l'observabilité intéressante
        try {
            Thread.sleep(100 + random.nextInt(1400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
