from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from prometheus_client import Counter, Histogram, generate_latest, CONTENT_TYPE_LATEST
from starlette.responses import Response
import httpx
import time
import random
import logging
from typing import Optional
from .models import HealthRecommendation, AlertLevel

# Configuration du logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Health Recommendations Service",
    description="Service de recommandations santé basé sur la qualité de l'air et la météo",
    version="1.0.0"
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuration des services externes
AIR_QUALITY_SERVICE = "http://air-quality-service:8080"
WEATHER_SERVICE = "http://weather-service:8081"

# Métriques Prometheus
recommendation_counter = Counter(
    'health_recommendations_total',
    'Total number of health recommendations generated',
    ['alert_level']
)

api_calls_counter = Counter(
    'health_api_calls_total',
    'Total number of API calls to external services',
    ['service', 'status']
)

api_latency = Histogram(
    'health_api_latency_seconds',
    'Latency of API calls to external services',
    ['service'],
    buckets=[0.1, 0.3, 0.5, 0.7, 1, 2, 5]
)

recommendation_latency = Histogram(
    'health_recommendation_latency_seconds',
    'Latency of recommendation generation',
    buckets=[0.1, 0.3, 0.5, 0.7, 1, 2, 5]
)


def simulate_latency():
    """Simule une latence variable pour la formation"""
    delay = random.uniform(0.05, 0.2)
    time.sleep(delay)


async def fetch_air_quality(city: str, country: str = "FR") -> dict:
    """Récupère les données de qualité de l'air"""
    logger.info(f"Fetching air quality data for {city}, {country}")
    start_time = time.time()

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{AIR_QUALITY_SERVICE}/api/air-quality/city",
                params={"city": city, "country": country}
            )
            response.raise_for_status()

            api_calls_counter.labels(service='air-quality', status='success').inc()
            api_latency.labels(service='air-quality').observe(time.time() - start_time)

            return response.json()
    except Exception as e:
        logger.error(f"Error fetching air quality data: {str(e)}")
        api_calls_counter.labels(service='air-quality', status='error').inc()
        api_latency.labels(service='air-quality').observe(time.time() - start_time)
        raise


async def fetch_weather(city: str, country: str = "FR") -> dict:
    """Récupère les données météo"""
    logger.info(f"Fetching weather data for {city}, {country}")
    start_time = time.time()

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{WEATHER_SERVICE}/api/weather/city",
                params={"city": city, "country": country}
            )
            response.raise_for_status()

            api_calls_counter.labels(service='weather', status='success').inc()
            api_latency.labels(service='weather').observe(time.time() - start_time)

            return response.json()
    except Exception as e:
        logger.error(f"Error fetching weather data: {str(e)}")
        api_calls_counter.labels(service='weather', status='error').inc()
        api_latency.labels(service='weather').observe(time.time() - start_time)
        raise


def generate_recommendations(air_quality_data: list, weather_data: dict) -> HealthRecommendation:
    """Génère des recommandations santé basées sur les données"""
    simulate_latency()

    # Calcul de l'AQI moyen
    if not air_quality_data:
        aqi = 50
        quality_level = "Good"
    else:
        aqi = sum(item.get('aqi', 50) for item in air_quality_data) / len(air_quality_data)
        quality_level = air_quality_data[0].get('qualityLevel', 'Good')

    # Détermination du niveau d'alerte
    if aqi <= 50:
        alert_level = AlertLevel.LOW
    elif aqi <= 100:
        alert_level = AlertLevel.MODERATE
    elif aqi <= 150:
        alert_level = AlertLevel.HIGH
    elif aqi <= 200:
        alert_level = AlertLevel.VERY_HIGH
    else:
        alert_level = AlertLevel.EXTREME

    # Génération des recommandations
    recommendations = []
    risk_groups = []
    activities = []

    # Recommandations basées sur l'AQI
    if aqi <= 50:
        recommendations.append("✅ La qualité de l'air est excellente. Profitez des activités en plein air!")
        activities.append("Course à pied, vélo, sports extérieurs")
    elif aqi <= 100:
        recommendations.append("⚠️ La qualité de l'air est acceptable. La plupart des personnes peuvent sortir.")
        recommendations.append("Les personnes sensibles devraient limiter les efforts prolongés en extérieur.")
        risk_groups.append("Personnes asthmatiques")
        activities.append("Activités modérées en extérieur")
    elif aqi <= 150:
        recommendations.append("⚠️ Qualité de l'air préoccupante pour les groupes sensibles.")
        recommendations.append("Limitez les activités extérieures intenses et prolongées.")
        risk_groups.extend(["Enfants", "Personnes âgées", "Personnes asthmatiques"])
        activities.append("Activités légères en extérieur, privilégier l'intérieur")
    elif aqi <= 200:
        recommendations.append("🚨 Qualité de l'air mauvaise. Tout le monde peut ressentir des effets.")
        recommendations.append("Évitez les activités extérieures intenses.")
        recommendations.append("Portez un masque si vous devez sortir.")
        risk_groups.extend(["Tout le monde", "Surtout: enfants, personnes âgées, malades chroniques"])
        activities.append("Activités en intérieur uniquement")
    else:
        recommendations.append("🆘 ALERTE: Qualité de l'air dangereuse!")
        recommendations.append("Restez à l'intérieur et gardez les fenêtres fermées.")
        recommendations.append("Portez un masque N95 si vous devez absolument sortir.")
        risk_groups.append("Toute la population")
        activities.append("Restez à l'intérieur")

    # Recommandations basées sur la météo
    temperature = weather_data.get('temperature', 20)
    humidity = weather_data.get('humidity', 50)

    if temperature > 30:
        recommendations.append("🌡️ Température élevée: Hydratez-vous régulièrement.")
    elif temperature < 5:
        recommendations.append("❄️ Température basse: Couvrez-vous bien.")

    if humidity > 80:
        recommendations.append("💧 Humidité élevée: Peut aggraver les problèmes respiratoires.")

    # Compteur de recommandations
    recommendation_counter.labels(alert_level=alert_level.value).inc()

    return HealthRecommendation(
        alert_level=alert_level,
        aqi=round(aqi, 1),
        quality_level=quality_level,
        recommendations=recommendations,
        at_risk_groups=risk_groups,
        suggested_activities=activities,
        temperature=temperature,
        humidity=humidity,
        timestamp=weather_data.get('timestamp', '')
    )


@app.get("/api/health/recommendations")
async def get_recommendations(
    city: str = Query(..., description="Nom de la ville"),
    country: str = Query("FR", description="Code pays (ISO 2 lettres)")
) -> HealthRecommendation:
    """
    Génère des recommandations santé basées sur la qualité de l'air et la météo
    """
    logger.info(f"Generating recommendations for {city}, {country}")
    start_time = time.time()

    try:
        # Récupération des données en parallèle
        air_quality_data = await fetch_air_quality(city, country)
        weather_data = await fetch_weather(city, country)

        # Génération des recommandations
        recommendations = generate_recommendations(air_quality_data, weather_data)

        recommendation_latency.observe(time.time() - start_time)
        logger.info(f"Recommendations generated successfully in {time.time() - start_time:.2f}s")

        return recommendations

    except httpx.HTTPError as e:
        logger.error(f"HTTP error occurred: {str(e)}")
        raise HTTPException(
            status_code=503,
            detail=f"Error communicating with external services: {str(e)}"
        )
    except Exception as e:
        logger.error(f"Unexpected error: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Internal server error: {str(e)}"
        )


@app.get("/api/health/alert-status")
async def get_alert_status(
    city: str = Query(..., description="Nom de la ville"),
    country: str = Query("FR", description="Code pays")
):
    """
    Retourne uniquement le niveau d'alerte pour une ville
    """
    try:
        air_quality_data = await fetch_air_quality(city, country)

        if not air_quality_data:
            aqi = 50
            alert_level = AlertLevel.LOW
        else:
            aqi = sum(item.get('aqi', 50) for item in air_quality_data) / len(air_quality_data)

            if aqi <= 50:
                alert_level = AlertLevel.LOW
            elif aqi <= 100:
                alert_level = AlertLevel.MODERATE
            elif aqi <= 150:
                alert_level = AlertLevel.HIGH
            elif aqi <= 200:
                alert_level = AlertLevel.VERY_HIGH
            else:
                alert_level = AlertLevel.EXTREME

        return {
            "city": city,
            "country": country,
            "alert_level": alert_level.value,
            "aqi": round(aqi, 1)
        }

    except Exception as e:
        logger.error(f"Error getting alert status: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/metrics")
async def metrics():
    """Endpoint Prometheus metrics"""
    return Response(content=generate_latest(), media_type=CONTENT_TYPE_LATEST)


@app.get("/health")
async def health():
    """Health check endpoint"""
    return {"status": "OK", "service": "health-service"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8082)
