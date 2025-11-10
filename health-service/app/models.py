from pydantic import BaseModel
from typing import List
from enum import Enum


class AlertLevel(str, Enum):
    """Niveaux d'alerte pour la qualité de l'air"""
    LOW = "low"
    MODERATE = "moderate"
    HIGH = "high"
    VERY_HIGH = "very_high"
    EXTREME = "extreme"


class HealthRecommendation(BaseModel):
    """Modèle de recommandations santé"""
    alert_level: AlertLevel
    aqi: float
    quality_level: str
    recommendations: List[str]
    at_risk_groups: List[str]
    suggested_activities: List[str]
    temperature: float
    humidity: float
    timestamp: str

    class Config:
        json_schema_extra = {
            "example": {
                "alert_level": "moderate",
                "aqi": 75.5,
                "quality_level": "Moderate",
                "recommendations": [
                    "La qualité de l'air est acceptable",
                    "Les personnes sensibles devraient limiter les efforts prolongés"
                ],
                "at_risk_groups": ["Personnes asthmatiques"],
                "suggested_activities": ["Activités modérées en extérieur"],
                "temperature": 22.5,
                "humidity": 65,
                "timestamp": "2024-01-01T12:00:00Z"
            }
        }
