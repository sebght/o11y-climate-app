package com.formation.airquality.service;

import com.formation.airquality.model.AirQualityData;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class AirQualityService {
    private static final Logger logger = LoggerFactory.getLogger(AirQualityService.class);
    private final Counter apiCallCounter;
    private final Counter apiErrorCounter;
    private final Timer apiLatencyTimer;
    private final Random random = new Random();

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

    public AirQualityService(MeterRegistry meterRegistry) {
        // Métriques personnalisées
        this.apiCallCounter = Counter.builder("airquality.api.calls")
                .description("Nombre de requêtes de données de qualité de l'air")
                .tag("service", "air-quality")
                .register(meterRegistry);

        this.apiErrorCounter = Counter.builder("airquality.api.errors")
                .description("Nombre d'erreurs lors de la génération de données")
                .tag("service", "air-quality")
                .register(meterRegistry);

        this.apiLatencyTimer = Timer.builder("airquality.api.latency")
                .description("Latence de la génération de données de qualité de l'air")
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

                // Génération de données fictives au lieu d'appeler l'API
                List<AirQualityData> data = generateMockAirQualityData(city, country, latitude, longitude);

                if (data.isEmpty()) {
                    logger.warn("No air quality data generated for city: {}", city);
                }

                return data;
            } catch (Exception e) {
                logger.error("Error generating air quality data", e);
                apiErrorCounter.increment();
                throw new RuntimeException("Error generating air quality data: " + e.getMessage());
            }
        });
    }

    public List<AirQualityData> getLatestByCoordinates(double latitude, double longitude, int radius) {
        logger.info("Fetching air quality data for coordinates: {}, {}, radius: {}", latitude, longitude, radius);
        apiCallCounter.increment();

        return apiLatencyTimer.record(() -> {
            try {
                simulateLatency();

                // Génération de données fictives au lieu d'appeler l'API
                List<AirQualityData> data = generateMockAirQualityData("Unknown", "FR", latitude, longitude);

                if (data.isEmpty()) {
                    logger.warn("No air quality data generated for coordinates: {}, {}", latitude, longitude);
                }

                return data;
            } catch (Exception e) {
                logger.error("Error generating air quality data by coordinates", e);
                apiErrorCounter.increment();
                throw new RuntimeException("Error generating air quality data: " + e.getMessage());
            }
        });
    }

    /**
     * Génère des données de qualité de l'air fictives pour une ville
     * Les données sont cohérentes avec la ville et la saison, mais varient à chaque appel
     */
    private List<AirQualityData> generateMockAirQualityData(String city, String country, double latitude, double longitude) {
        List<AirQualityData> data = new ArrayList<>();

        // Paramètres de qualité de l'air à simuler
        String[] parameters = {"pm25", "pm10", "no2", "o3", "so2", "co"};
        String[] units = {"µg/m³", "µg/m³", "µg/m³", "µg/m³", "µg/m³", "mg/m³"};

        // Valeurs de base pour janvier (hiver) - pollution potentiellement plus élevée
        // Valeurs légèrement augmentées en fonction de la latitude (plus au nord = plus de chauffage)
        double latitudeFactor = (latitude - 40) / 10.0; // Factor between 0 and 1 for French cities

        for (int i = 0; i < parameters.length; i++) {
            AirQualityData aqData = new AirQualityData();
            aqData.setCity(city);
            aqData.setCountry(country);
            aqData.setLatitude(latitude);
            aqData.setLongitude(longitude);
            aqData.setParameter(parameters[i]);
            aqData.setUnit(units[i]);

            // Générer une valeur avec variation aléatoire
            double baseValue;
            switch (parameters[i]) {
                case "pm25":
                    // PM2.5: 10-40 µg/m³ en hiver (légèrement pollué)
                    baseValue = 15 + (random.nextDouble() * 25) + (latitudeFactor * 5);
                    break;
                case "pm10":
                    // PM10: 20-60 µg/m³
                    baseValue = 25 + (random.nextDouble() * 35) + (latitudeFactor * 10);
                    break;
                case "no2":
                    // NO2: 20-80 µg/m³ (plus élevé en ville)
                    baseValue = 30 + (random.nextDouble() * 50);
                    break;
                case "o3":
                    // O3: 40-80 µg/m³
                    baseValue = 40 + (random.nextDouble() * 40);
                    break;
                case "so2":
                    // SO2: 5-20 µg/m³
                    baseValue = 5 + (random.nextDouble() * 15);
                    break;
                case "co":
                    // CO: 0.2-0.8 mg/m³
                    baseValue = 0.2 + (random.nextDouble() * 0.6);
                    break;
                default:
                    baseValue = 50;
            }

            // Arrondir à 2 décimales
            double value = Math.round(baseValue * 100.0) / 100.0;
            aqData.setValue(value);

            // Calculer l'AQI et le niveau de qualité
            int aqi = calculateAQI(parameters[i], value);
            aqData.setAqi(aqi);
            aqData.setQualityLevel(getQualityLevel(aqi));

            // Timestamp actuel
            aqData.setLastUpdated(java.time.Instant.now().toString());

            data.add(aqData);
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
