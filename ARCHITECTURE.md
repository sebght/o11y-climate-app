# 🏗️ Architecture de l'Application

Documentation technique détaillée de l'architecture du Dashboard Qualité de l'Air.

## Vue d'Ensemble

L'application suit une architecture microservices avec 3 services backend, un frontend, et une stack d'observabilité (Prometheus + Grafana).

## Diagramme d'Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         FRONTEND                            │
│                      (Nginx + SPA)                          │
│                        Port 80                              │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
┌───────────────┐ ┌───────────┐ ┌────────────────┐
│ Air Quality   │ │  Weather  │ │    Health      │
│   Service     │ │  Service  │ │   Service      │
│  (Spring)     │ │ (Node.js) │ │  (FastAPI)     │
│   :8080       │ │   :8081   │ │    :8082       │
└───────┬───────┘ └─────┬─────┘ └────────┬───────┘
        │               │                 │
        │               │        ┌────────┴────────┐
        │               │        │                 │
        │               │        ▼                 ▼
        ▼               ▼   Air Quality       Weather
    OpenAQ API   OpenWeather     Service        Service
                    API

        ┌───────────────┬───────────────┬───────────┐
        │               │               │           │
        ▼               ▼               ▼           │
┌───────────────────────────────────────────────┐  │
│              PROMETHEUS                       │  │
│           (Metrics Collector)                 │  │
│              :9090                            │  │
└──────────────────┬────────────────────────────┘  │
                   │                               │
                   ▼                               │
         ┌──────────────────┐                     │
         │     GRAFANA      │◄────────────────────┘
         │  (Dashboards)    │
         │      :3000       │
         └──────────────────┘
```

## Services Backend

### 1. Air Quality Service (Spring Boot)

**Responsabilité** : Fournir les données de qualité de l'air

**Stack Technique** :
- Spring Boot 3.2.0
- Java 17
- Maven
- Micrometer (métriques)
- WebClient (appels HTTP)

**Endpoints** :
- `GET /api/air-quality/city` - Données par ville
- `GET /api/air-quality/coordinates` - Données par coordonnées
- `GET /api/air-quality/health` - Health check
- `GET /actuator/prometheus` - Métriques

**API Externe** :
- OpenAQ API v2 (https://api.openaq.org/v2)
- Gratuite, sans clé requise
- Rate limit : Selon usage raisonnable

**Métriques Exposées** :
```
airquality_api_calls_total - Counter
airquality_api_errors_total - Counter
airquality_api_latency_seconds - Timer
airquality_city_request_seconds - Timer
jvm_* - Métriques JVM standard
```

**Flux de Données** :
1. Requête HTTP entrante
2. Appel à OpenAQ API
3. Parsing et calcul AQI
4. Enrichissement des données
5. Réponse JSON + métriques

### 2. Weather Service (Node.js)

**Responsabilité** : Fournir les données météorologiques

**Stack Technique** :
- Node.js 18
- Express.js
- Axios (appels HTTP)
- prom-client (métriques Prometheus)

**Endpoints** :
- `GET /api/weather/city` - Météo par ville
- `GET /api/weather/coordinates` - Météo par coordonnées
- `GET /api/weather/forecast` - Prévisions
- `GET /health` - Health check
- `GET /metrics` - Métriques Prometheus

**API Externe** :
- OpenWeatherMap API
- Clé API requise (gratuite : 60 req/min)
- Inscription : https://openweathermap.org/api

**Métriques Exposées** :
```
weather_api_calls_total - Counter
weather_api_latency_seconds - Histogram
weather_http_request_duration_seconds - Histogram
nodejs_* - Métriques Node.js standard
```

**Flux de Données** :
1. Requête HTTP entrante
2. Validation des paramètres
3. Appel à OpenWeatherMap API
4. Transformation des données
5. Réponse JSON + métriques

### 3. Health Service (FastAPI)

**Responsabilité** : Générer des recommandations santé

**Stack Technique** :
- Python 3.11
- FastAPI
- httpx (appels HTTP async)
- prometheus-client (métriques)

**Endpoints** :
- `GET /api/health/recommendations` - Recommandations complètes
- `GET /api/health/alert-status` - Niveau d'alerte uniquement
- `GET /health` - Health check
- `GET /metrics` - Métriques Prometheus

**Dépendances** :
- Air Quality Service (pour données AQI)
- Weather Service (pour données météo)

**Métriques Exposées** :
```
health_recommendations_total - Counter
health_api_calls_total - Counter
health_api_latency_seconds - Histogram
health_recommendation_latency_seconds - Histogram
```

**Flux de Données** :
1. Requête HTTP entrante
2. Appels parallèles aux services Air Quality et Weather
3. Agrégation des données
4. Analyse et génération de recommandations
5. Calcul du niveau d'alerte
6. Réponse JSON + métriques

**Logique de Recommandations** :
```python
AQI <= 50     → Alert Level: LOW      → "Qualité excellente"
AQI <= 100    → Alert Level: MODERATE → "Acceptable"
AQI <= 150    → Alert Level: HIGH     → "Préoccupant"
AQI <= 200    → Alert Level: VERY_HIGH → "Mauvais"
AQI > 200     → Alert Level: EXTREME  → "Dangereux"
```

## Frontend

**Stack Technique** :
- HTML5 / CSS3 / JavaScript Vanilla
- Leaflet.js (carte interactive)
- Nginx (serveur web)

**Fonctionnalités** :
- Carte interactive mondiale
- Recherche par ville
- Villes pré-configurées (Paris, Londres, Tokyo, etc.)
- Affichage en temps réel :
  - Qualité de l'air avec AQI
  - Conditions météorologiques
  - Recommandations santé avec niveau d'alerte
- Status des services

**Architecture Frontend** :
```
frontend/
├── index.html              # Page principale
├── assets/
│   ├── css/
│   │   └── style.css      # Styles
│   └── js/
│       └── app.js         # Logique applicative
└── Dockerfile             # Configuration Nginx
```

## Observabilité

### Prometheus

**Rôle** : Collecte et stockage des métriques

**Configuration** :
- Scraping interval : 15s
- Retention : 15 jours (par défaut)
- Targets :
  - air-quality-service:8080
  - weather-service:8081
  - health-service:8082

**Format des Métriques** :
```
# TYPE metric_name counter|gauge|histogram
# HELP metric_name Description of the metric
metric_name{label1="value1",label2="value2"} value timestamp
```

### Grafana

**Rôle** : Visualisation des métriques

**Configuration** :
- Source de données : Prometheus (pré-configurée)
- Identifiants par défaut : admin/admin
- Provisioning automatique des datasources

**Dashboards Recommandés** :
1. **Vue d'ensemble** : Santé globale
2. **Services** : Métriques par service
3. **Performance** : Latence et throughput
4. **Erreurs** : Taux d'erreur et incidents
5. **Business** : Métriques métier

## Modèles de Données

### Air Quality Data

```json
{
  "city": "Paris",
  "country": "FR",
  "latitude": 48.8566,
  "longitude": 2.3522,
  "parameter": "pm25",
  "value": 15.2,
  "unit": "µg/m³",
  "lastUpdated": "2024-01-01T12:00:00Z",
  "aqi": 58,
  "qualityLevel": "Moderate"
}
```

### Weather Data

```json
{
  "city": "Paris",
  "country": "FR",
  "temperature": 18.5,
  "feelsLike": 17.2,
  "humidity": 65,
  "pressure": 1013,
  "description": "nuageux",
  "windSpeed": 5.2,
  "windDirection": 180,
  "clouds": 40,
  "visibility": 10000,
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### Health Recommendation

```json
{
  "alert_level": "moderate",
  "aqi": 75.5,
  "quality_level": "Moderate",
  "recommendations": [
    "La qualité de l'air est acceptable",
    "Les personnes sensibles devraient limiter..."
  ],
  "at_risk_groups": ["Personnes asthmatiques"],
  "suggested_activities": ["Activités modérées en extérieur"],
  "temperature": 18.5,
  "humidity": 65,
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## Déploiement

### Docker Compose

Architecture multi-conteneurs :
- Network bridge `app-network`
- Volumes persistants pour Prometheus et Grafana
- Health checks sur chaque service
- Dépendances explicites entre services

### Ordre de Démarrage

1. Air Quality Service
2. Weather Service
3. Health Service (dépend de 1 et 2)
4. Frontend (dépend de tous)
5. Prometheus (scrape tous les services)
6. Grafana (dépend de Prometheus)

### Ressources Requises

- CPU : 2 cores minimum
- RAM : 4 GB minimum (8 GB recommandés)
- Disk : 2 GB pour les images + volumes

## Sécurité

### Points d'Attention

1. **API Keys** :
   - OpenWeatherMap : Stockée dans .env
   - Jamais committée dans Git

2. **CORS** :
   - Activé sur tous les services backend
   - En production : restreindre les origins

3. **Secrets** :
   - Pas de secrets hardcodés
   - Utiliser des variables d'environnement

4. **Réseau** :
   - Services backend non exposés directement
   - Frontend = seul point d'entrée

## Monitoring et Alertes

### SLIs Recommandés

1. **Availability** : % de requêtes réussies (> 99.5%)
2. **Latency** : P95 < 500ms
3. **Throughput** : Requêtes par seconde
4. **Error Rate** : < 0.5%

### Alertes Critiques

1. Service down (health check fails)
2. Taux d'erreur > 5%
3. Latence P95 > 1s
4. API externe injoignable

### Dashboards

1. **Golden Signals** : Latency, Traffic, Errors, Saturation
2. **RED Metrics** : Rate, Errors, Duration
3. **USE Metrics** : Utilization, Saturation, Errors

## Évolutions Futures

### Court Terme

1. Ajouter des traces (OpenTelemetry)
2. Implémenter des alertes Grafana
3. Ajouter des logs structurés (Loki)
4. Circuit breaker pattern

### Moyen Terme

1. Ajout d'une base de données
2. Historique des données
3. API GraphQL
4. Authentication/Authorization

### Long Terme

1. Machine Learning pour prédictions
2. Mobile app (React Native)
3. Multi-région deployment
4. Kubernetes migration

## Références

- [12-Factor App](https://12factor.net/)
- [Microservices Patterns](https://microservices.io/patterns/)
- [Observability Engineering](https://www.oreilly.com/library/view/observability-engineering/9781492076438/)
- [SRE Book (Google)](https://sre.google/books/)

---

Pour toute question sur l'architecture, consultez la documentation complète dans [README.md](README.md).
