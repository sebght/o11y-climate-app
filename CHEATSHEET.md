# 📝 Cheatsheet - Commandes Essentielles

Référence rapide des commandes les plus utiles pour ce projet.

## 🚀 Démarrage

```bash
# Configuration initiale (interactive)
./scripts/init-env.sh

# Démarrage simple
docker-compose up -d

# Ou avec Makefile
make start

# Avec rebuild
docker-compose up -d --build
```

## 🛑 Arrêt

```bash
# Arrêt simple
docker-compose down

# Arrêt + suppression des volumes
docker-compose down -v

# Ou avec Makefile
make stop
make clean  # Avec volumes
```

## 📊 Monitoring

```bash
# Status des conteneurs
docker-compose ps

# Logs de tous les services
docker-compose logs -f

# Logs d'un service spécifique
docker-compose logs -f weather-service
docker-compose logs -f air-quality-service
docker-compose logs -f health-service

# Health checks
make health
```

## 🧪 Tests

```bash
# Test de tous les services
./scripts/test-all-services.sh

# Test manuel d'un endpoint
curl "http://localhost:8080/api/air-quality/city?city=Paris&country=FR"
curl "http://localhost:8081/api/weather/city?city=Paris&country=FR"
curl "http://localhost:8082/api/health/recommendations?city=Paris&country=FR"

# Test rapide Paris
make test-paris
```

## ⚡ Génération de Charge

```bash
# Installation de 'hey'
# macOS
brew install hey

# Linux
go install github.com/rakyll/hey@latest

# Génération de charge (60 secondes)
./scripts/generate-load.sh 60

# Charge spécifique sur un service
hey -n 1000 -c 10 "http://localhost:8081/api/weather/city?city=Paris&country=FR"

# Avec Makefile
make test-load
```

## 📈 Métriques

```bash
# Accès aux métriques
curl http://localhost:8080/actuator/prometheus  # Spring Boot
curl http://localhost:8081/metrics              # Node.js
curl http://localhost:8082/metrics              # FastAPI

# Endpoints Prometheus
open http://localhost:9090

# Targets
open http://localhost:9090/targets

# Graph
open http://localhost:9090/graph
```

## 📊 PromQL - Requêtes Utiles

```promql
# Taux de requêtes par seconde
rate(weather_http_request_duration_seconds_count[5m])

# Latence moyenne
rate(airquality_api_latency_seconds_sum[5m]) / rate(airquality_api_latency_seconds_count[5m])

# Percentile 95
histogram_quantile(0.95, rate(weather_api_latency_seconds_bucket[5m]))

# Taux d'erreur
rate(airquality_api_errors_total[5m]) / rate(airquality_api_calls_total[5m]) * 100

# Recommandations par niveau
sum by (alert_level) (health_recommendations_total)

# Top 5 des villes
topk(5, weather_city_requests_total)

# Uptime (gauge metric)
up{job="weather-service"}
```

## 🎨 Grafana

```bash
# Accès
open http://localhost:3000

# Identifiants par défaut
Username: admin
Password: admin

# Import d'un dashboard
1. + → Import
2. Upload JSON ou ID Grafana.com
3. Sélectionner datasource: Prometheus

# Dashboards publics recommandés
- Node Exporter: 1860
- Spring Boot: 12900
- Docker: 893
```

## 🔧 Debugging

```bash
# Inspecter un conteneur
docker-compose exec weather-service sh

# Variables d'environnement d'un service
docker-compose exec weather-service env

# Restart d'un service
docker-compose restart weather-service

# Rebuild d'un service spécifique
docker-compose build --no-cache weather-service
docker-compose up -d weather-service

# Voir les ressources utilisées
docker stats

# Nettoyer Docker
docker system prune -a
```

## 🌍 Endpoints Frontend

```bash
# Page principale
open http://localhost

# Status des services (visible dans le footer)
```

## 🔑 Variables d'Environnement

```bash
# Éditer la configuration
nano .env

# Variables importantes
OPENWEATHER_API_KEY=your_key_here
AIR_QUALITY_PORT=8080
WEATHER_PORT=8081
HEALTH_PORT=8082
```

## 📦 Docker Compose

```bash
# Scaler un service
docker-compose up -d --scale weather-service=3

# Voir les dépendances
docker-compose config

# Valider le fichier
docker-compose config --quiet

# Recréer les conteneurs
docker-compose up -d --force-recreate

# Voir les volumes
docker volume ls

# Supprimer un volume spécifique
docker volume rm o11y-bpi-france_prometheus-data
```

## 🎯 Services Individuels

```bash
# Air Quality Service
docker-compose up -d air-quality-service
docker-compose logs -f air-quality-service
docker-compose restart air-quality-service

# Weather Service
docker-compose up -d weather-service
docker-compose logs -f weather-service
docker-compose restart weather-service

# Health Service
docker-compose up -d health-service
docker-compose logs -f health-service
docker-compose restart health-service

# Frontend
docker-compose up -d frontend
docker-compose logs -f frontend
docker-compose restart frontend
```

## 🔍 Troubleshooting

```bash
# Port déjà utilisé
lsof -i :8080
kill -9 <PID>

# Conteneur qui ne démarre pas
docker-compose logs service-name
docker-compose up service-name  # Sans -d pour voir les erreurs

# Réseau Docker
docker network ls
docker network inspect o11y-bpi-france_app-network

# Cleanup complet
docker-compose down -v
docker system prune -a
docker volume prune
```

## 📚 Développement Local (sans Docker)

```bash
# Air Quality Service
cd air-quality-service
./mvnw spring-boot:run

# Weather Service
cd weather-service
npm install
npm start

# Health Service
cd health-service
pip install -r requirements.txt
uvicorn app.main:app --reload

# Frontend
cd frontend
python3 -m http.server 8000
```

## 🎓 Exercices Formation

```bash
# Voir les exercices
cat TRAINING.md

# Exercice 1: Explorer les métriques
open http://localhost:9090
# Essayer: rate(weather_http_request_duration_seconds_count[5m])

# Exercice 2: Créer un dashboard
open http://localhost:3000
# Dashboard → New → Add Panel

# Exercice 3: Simuler une panne
docker-compose stop weather-service
# Observer l'impact
docker-compose start weather-service

# Exercice 4: Analyse de charge
./scripts/generate-load.sh 60
# Observer dans Grafana
```

## 🔗 URLs Rapides

```bash
# Services
http://localhost              # Frontend
http://localhost:8080         # Air Quality API
http://localhost:8081         # Weather API
http://localhost:8082         # Health API

# Monitoring
http://localhost:9090         # Prometheus
http://localhost:9090/targets # Targets
http://localhost:3000         # Grafana

# Health Checks
http://localhost:8080/api/air-quality/health
http://localhost:8081/health
http://localhost:8082/health

# Métriques
http://localhost:8080/actuator/prometheus
http://localhost:8081/metrics
http://localhost:8082/metrics
```

## 📖 Documentation

```bash
# Lire la doc
cat README.md           # Documentation complète
cat QUICKSTART.md       # Démarrage rapide
cat TRAINING.md         # Exercices
cat ARCHITECTURE.md     # Architecture détaillée

# Ouvrir dans le navigateur
open README.md
```

## 🆘 Aide

```bash
# Make help
make help

# Docker Compose help
docker-compose --help

# Version info
docker --version
docker-compose --version
```

## 🎯 One-Liners Utiles

```bash
# Test rapide de tous les health checks
for port in 8080 8081 8082; do curl -s http://localhost:$port/health | jq; done

# Restart de tous les services
docker-compose restart

# Logs condensés (dernières 50 lignes)
docker-compose logs --tail=50

# Suivre les logs avec timestamps
docker-compose logs -f -t

# Statistiques en temps réel
watch -n 1 'docker stats --no-stream'

# Compter les requêtes dans les logs
docker-compose logs weather-service | grep "GET" | wc -l
```

---

💡 **Astuce**: Utilisez `make help` pour voir toutes les commandes disponibles !
