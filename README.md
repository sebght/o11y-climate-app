# 🌍 Dashboard Qualité de l'Air Mondial - Formation Observabilité

Application de démonstration pour une formation sur l'observabilité et OpenTelemetry avec des développeurs fullstack.

## 📋 Vue d'ensemble

Cette application est composée de **3 microservices** et d'un **front-end** qui permettent de visualiser en temps réel la qualité de l'air et les conditions météorologiques dans le monde entier, avec des recommandations santé associées.

### Architecture

```
┌─────────────────┐
│    Frontend     │  ← Nginx + Leaflet.js (Map interactive)
│   (Port 80)     │
└────────┬────────┘
         │
         ├─────────────┬─────────────┬─────────────┐
         │             │             │             │
         ▼             ▼             ▼             │
┌─────────────┐ ┌───────────┐ ┌─────────────┐    │
│ Air Quality │ │  Weather  │ │   Health    │    │
│  Service    │ │  Service  │ │  Service    │    │
│  (Spring)   │ │ (Node.js) │ │  (FastAPI)  │    │
│  Port 8080  │ │ Port 8081 │ │  Port 8082  │    │
└──────┬──────┘ └─────┬─────┘ └──────┬──────┘    │
       │              │                │          │
       │              │                │          │
       ▼              ▼                ▼          ▼
   Mock Data     Mock Data       Appels aux
   Generator     Generator       autres services

         ┌──────────────────────────────┐
         │                              │
         ▼                              ▼
   ┌──────────┐                  ┌──────────┐
   │Prometheus│                  │ Grafana  │
   │Port 9090 │◄─────────────────│Port 3000 │
   └──────────┘                  └──────────┘
```

### Services

1. **🌬️ Air Quality Service (Spring Boot)** - Port 8080
   - Génère des données fictives de qualité de l'air cohérentes
   - Calcule l'AQI (Air Quality Index)
   - Expose des métriques Prometheus via Micrometer
   - Données réalistes qui varient à chaque requête

2. **🌤️ Weather Service (Node.js)** - Port 8081
   - Génère des données météorologiques fictives cohérentes
   - Fournit température, humidité, vent, etc.
   - Expose des métriques Prometheus via prom-client
   - Données adaptées à la saison actuelle (hiver)

3. **🏥 Health Service (FastAPI)** - Port 8082
   - Génère des recommandations santé basées sur l'air et la météo
   - Agrège les données des deux autres services
   - Expose des métriques Prometheus

4. **🖥️ Frontend** - Port 80
   - Interface web avec carte interactive (Leaflet.js)
   - Visualisation en temps réel
   - Affichage des recommandations

5. **📊 Observabilité**
   - **Prometheus** (Port 9090) : Collecte des métriques
   - **Grafana** (Port 3000) : Visualisation des métriques

## 🚀 Installation et Démarrage

### Prérequis

- Docker ou Podman
- Docker Compose

**Note:** Les services utilisent maintenant des **données fictives générées localement**. Aucune clé API externe n'est nécessaire, ce qui permet un load testing illimité sans quotas.

### Configuration

### Démarrage avec Docker Compose

Aucune configuration supplémentaire n'est nécessaire. Les services génèrent automatiquement des données fictives.

```bash
# Démarrer tous les services
docker-compose up -d

# Voir les logs
docker-compose logs -f

# Arrêter tous les services
docker-compose down

# Arrêter et supprimer les volumes
docker-compose down -v
```

### Démarrage avec Podman

```bash
# Remplacer docker-compose par podman-compose
podman-compose up -d

# Voir les logs
podman-compose logs -f

# Arrêter
podman-compose down
```

## 🌐 Accès aux Services

Une fois démarrés, les services sont accessibles aux URLs suivantes :

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost | Interface utilisateur principale |
| **Air Quality API** | http://localhost:8080 | API qualité de l'air |
| **Weather API** | http://localhost:8081 | API météo |
| **Health API** | http://localhost:8082 | API recommandations santé |
| **Prometheus** | http://localhost:9090 | Interface Prometheus |
| **Grafana** | http://localhost:3000 | Dashboards Grafana (admin/admin) |

### Endpoints API

#### Air Quality Service (8080)

```bash
# Par ville
curl "http://localhost:8080/api/air-quality/city?city=Paris&country=FR"

# Par coordonnées
curl "http://localhost:8080/api/air-quality/coordinates?latitude=48.8566&longitude=2.3522&radius=25000"

# Health check
curl "http://localhost:8080/api/air-quality/health"

# Métriques
curl "http://localhost:8080/actuator/prometheus"
```

#### Weather Service (8081)

```bash
# Par ville
curl "http://localhost:8081/api/weather/city?city=Paris&country=FR"

# Par coordonnées
curl "http://localhost:8081/api/weather/coordinates?latitude=48.8566&longitude=2.3522"

# Prévisions
curl "http://localhost:8081/api/weather/forecast?city=Paris&country=FR&days=5"

# Health check
curl "http://localhost:8081/health"

# Métriques
curl "http://localhost:8081/metrics"
```

#### Health Service (8082)

```bash
# Recommandations complètes
curl "http://localhost:8082/api/health/recommendations?city=Paris&country=FR"

# Statut d'alerte uniquement
curl "http://localhost:8082/api/health/alert-status?city=Paris&country=FR"

# Health check
curl "http://localhost:8082/health"

# Métriques
curl "http://localhost:8082/metrics"
```

## 📊 Observabilité

### Métriques disponibles

Chaque service expose des métriques au format Prometheus :

#### Air Quality Service (Spring Boot)

- `airquality_api_calls_total` - Nombre total d'appels à l'API OpenAQ
- `airquality_api_errors_total` - Nombre d'erreurs API
- `airquality_api_latency_seconds` - Latence des appels API
- `airquality_city_request_seconds` - Durée des requêtes par ville
- Métriques JVM standard (mémoire, threads, GC, etc.)

#### Weather Service (Node.js)

- `weather_api_calls_total` - Nombre d'appels à OpenWeatherMap
- `weather_api_latency_seconds` - Latence des appels API
- `weather_http_request_duration_seconds` - Durée des requêtes HTTP
- Métriques Node.js standard (event loop, mémoire, etc.)

#### Health Service (FastAPI)

- `health_recommendations_total` - Nombre de recommandations générées
- `health_api_calls_total` - Appels aux services externes
- `health_api_latency_seconds` - Latence des appels
- `health_recommendation_latency_seconds` - Durée de génération des recommandations

### Visualisation avec Grafana

1. Accédez à Grafana : http://localhost:3000
2. Connectez-vous avec `admin` / `admin`
3. La source de données Prometheus est déjà configurée
4. Créez vos propres dashboards ou importez des templates

**Exemples de requêtes PromQL utiles :**

```promql
# Taux d'erreur API par service
rate(airquality_api_errors_total[5m]) / rate(airquality_api_calls_total[5m]) * 100

# Latence P95 du service weather
histogram_quantile(0.95, rate(weather_api_latency_seconds_bucket[5m]))

# Nombre de recommandations par niveau d'alerte
sum by (alert_level) (health_recommendations_total)

# Taux de requêtes HTTP par seconde
rate(weather_http_request_duration_seconds_count[1m])
```

### Traces et Logs

Les services génèrent des logs structurés visibles avec :

```bash
# Tous les services
docker-compose logs -f

# Un service spécifique
docker-compose logs -f air-quality-service
docker-compose logs -f weather-service
docker-compose logs -f health-service
```

## 🎓 Points d'Intérêt pour la Formation

### 1. Métriques Personnalisées

Chaque service implémente des métriques personnalisées :

- **Counters** : Comptage d'événements (appels API, erreurs)
- **Histograms** : Distribution des valeurs (latence, durée)
- **Gauges** : Valeurs instantanées (via les métriques système)

### 2. Latence Variable

Les services simulent une latence variable (100-500ms) pour rendre l'observabilité intéressante et visualiser :
- Les percentiles (P50, P95, P99)
- Les pics de latence
- L'impact sur les services dépendants

### 3. Appels API Externes

Chaque service appelle des APIs externes, permettant d'observer :
- Les timeouts
- Les échecs réseau
- Les retry strategies
- La propagation d'erreurs

### 4. Architecture Microservices

Le Health Service dépend des deux autres services, permettant d'étudier :
- La propagation des erreurs
- Les cascading failures
- Les circuit breakers (à implémenter)
- La résilience

### 5. Technologies Hétérogènes

3 langages/frameworks différents montrent comment instrumenter :
- **Spring Boot** : Micrometer + Spring Actuator
- **Node.js** : prom-client
- **FastAPI** : prometheus-client

## 🛠️ Développement Local

### Air Quality Service (Spring Boot)

```bash
cd air-quality-service
./mvnw spring-boot:run
```

### Weather Service (Node.js)

```bash
cd weather-service
npm install
npm start
```

### Health Service (FastAPI)

```bash
cd health-service
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8082 --reload
```

### Frontend

```bash
cd frontend
# Servir avec un serveur HTTP simple
python3 -m http.server 8000
```

## 🧪 Tests

### Tester la disponibilité des services

```bash
# Script de health check
for service in 8080 8081 8082; do
  echo "Testing port $service..."
  curl -s http://localhost:$service/health | jq
done
```

### Générer de la charge

```bash
# Installer k6 (HTTP load generator)
# macOS: brew install k6
# Linux: https://k6.io/docs/get-started/installation/
# Windows: choco install k6

# Générer de la charge avec le script fourni (durée: 60s)
./scripts/generate-load.sh

# Ou directement avec k6
k6 run scripts/load-test.js
```

**⚠️ Important** : Le test est configuré pour respecter les quotas API (OpenWeather: 1000/jour, OpenAQ: 60/minute).
- 0.3 req/s sur health-service → 18 appels en 60s
- Vous pouvez lancer le test **~55 fois par jour**
- Voir [LOAD_TESTING.md](LOAD_TESTING.md) pour plus de détails

## 📚 Ressources Supplémentaires

### APIs Utilisées

- **OpenAQ** : https://docs.openaq.org/
  - API gratuite, sans clé requise
  - Données de qualité de l'air en temps réel

- **OpenWeatherMap** : https://openweathermap.org/api
  - Clé API gratuite (60 appels/minute)
  - Données météo mondiales

### Documentation OpenTelemetry

- Site officiel : https://opentelemetry.io/
- Spring Boot : https://opentelemetry.io/docs/instrumentation/java/
- Node.js : https://opentelemetry.io/docs/instrumentation/js/
- Python : https://opentelemetry.io/docs/instrumentation/python/

### Prometheus & Grafana

- Prometheus : https://prometheus.io/docs/
- Grafana : https://grafana.com/docs/
- PromQL : https://prometheus.io/docs/prometheus/latest/querying/basics/

## 🐛 Troubleshooting

### Les services ne démarrent pas

```bash
# Vérifier les logs
docker-compose logs

# Vérifier les ports utilisés
netstat -an | grep LISTEN | grep -E "8080|8081|8082|9090|3000"

# Nettoyer et redémarrer
docker-compose down -v
docker-compose up -d --build
```

### Le service Spring Boot est lent à démarrer

Le service Spring Boot peut prendre 30-60 secondes pour démarrer complètement. C'est normal pour une première build Maven.

### Erreur "Weather API key not configured"

Si vous n'avez pas de clé OpenWeatherMap, certaines fonctionnalités seront limitées. Obtenez une clé gratuite sur https://openweathermap.org/api et configurez-la dans `.env`.

### Prometheus ne collecte pas les métriques

```bash
# Vérifier la configuration Prometheus
curl http://localhost:9090/api/v1/targets

# Vérifier que les services exposent bien leurs métriques
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8081/metrics
curl http://localhost:8082/metrics
```

## 📝 Exercices Pratiques pour la Formation

### Exercice 1 : Explorer les métriques de base
- Accédez à Prometheus et explorez les métriques disponibles
- Créez des requêtes PromQL simples pour visualiser les données

### Exercice 2 : Créer un dashboard Grafana
- Créez un dashboard montrant :
  - Le taux de requêtes par seconde
  - La latence P95 de chaque service
  - Le taux d'erreur

### Exercice 3 : Simuler une panne
- Arrêtez un service : `docker-compose stop weather-service`
- Observez l'impact sur le Health Service
- Analysez les métriques d'erreur

### Exercice 4 : Ajouter une métrique personnalisée
- Ajoutez une nouvelle métrique dans un service
- Vérifiez qu'elle apparaît dans Prometheus
- Créez une visualisation dans Grafana

### Exercice 5 : Analyser les performances
- Générez de la charge avec `k6`
- Observez les métriques en temps réel
- Identifiez les goulots d'étranglement

## 📄 Licence

Ce projet est fourni à des fins éducatives pour la formation BPI France sur l'observabilité et OpenTelemetry.

## 👥 Auteur

Formation Observabilité & OpenTelemetry - BPI France 2024
