# 🎓 Guide de Formation - Observabilité et OpenTelemetry

Exercices pratiques pour apprendre l'observabilité avec cette application.

## Table des Matières

1. [Introduction à l'Observabilité](#1-introduction-à-lobservabilité)
2. [Exploration des Métriques](#2-exploration-des-métriques)
3. [Création de Dashboards](#3-création-de-dashboards)
4. [Analyse des Performances](#4-analyse-des-performances)
5. [Gestion des Incidents](#5-gestion-des-incidents)
6. [Métriques Personnalisées](#6-métriques-personnalisées)

---

## 1. Introduction à l'Observabilité

### Objectifs
- Comprendre les 3 piliers de l'observabilité (logs, métriques, traces)
- Découvrir Prometheus et Grafana
- Explorer l'architecture de l'application

### Exercice 1.1 : Découverte de l'Architecture

**Tâche** : Identifier tous les services et leurs dépendances

1. Démarrez l'application : `make start`
2. Listez tous les conteneurs : `docker-compose ps`
3. Dessinez un diagramme montrant :
   - Les 3 microservices
   - Leurs ports
   - Leurs dépendances
   - Les services d'observabilité

**Questions** :
- Quels services sont indépendants ?
- Quel service dépend des autres ?
- Pourquoi cette architecture est-elle intéressante pour l'observabilité ?

### Exercice 1.2 : Premiers Logs

**Tâche** : Observer les logs des services

```bash
# Logs de tous les services
docker-compose logs -f

# Logs d'un service spécifique
docker-compose logs -f weather-service
```

**Questions** :
- Quelles informations voyez-vous dans les logs ?
- Comment les logs sont-ils structurés ?
- Peut-on identifier les requêtes entrantes ?

---

## 2. Exploration des Métriques

### Objectifs
- Comprendre le format Prometheus
- Explorer les métriques exposées par chaque service
- Utiliser PromQL pour requêter les métriques

### Exercice 2.1 : Métriques Exposées

**Tâche** : Explorer les endpoints de métriques

```bash
# Service Spring Boot
curl http://localhost:8080/actuator/prometheus

# Service Node.js
curl http://localhost:8081/metrics

# Service FastAPI
curl http://localhost:8082/metrics
```

**Questions** :
- Combien de métriques sont exposées par chaque service ?
- Quelles sont les métriques communes à tous les services ?
- Quelles sont les métriques spécifiques à chaque technologie ?

### Exercice 2.2 : Prometheus UI

**Tâche** : Utiliser l'interface Prometheus

1. Ouvrez Prometheus : http://localhost:9090
2. Allez dans **Status > Targets**
3. Vérifiez que tous les services sont "UP"

**Questions** :
- Quelle est la fréquence de scraping ?
- Tous les services sont-ils accessibles ?
- Que se passe-t-il si un service est down ?

### Exercice 2.3 : Requêtes PromQL Basiques

**Tâche** : Exécuter des requêtes dans Prometheus (Graph > Execute)

```promql
# 1. Nombre total d'appels API au service météo
weather_api_calls_total

# 2. Taux de requêtes par seconde (sur 5 minutes)
rate(weather_http_request_duration_seconds_count[5m])

# 3. Latence moyenne du service Air Quality
rate(airquality_api_latency_seconds_sum[5m]) / rate(airquality_api_latency_seconds_count[5m])

# 4. Nombre de recommandations par niveau d'alerte
sum by (alert_level) (health_recommendations_total)
```

**Questions** :
- Quelle est la différence entre un counter et un gauge ?
- Pourquoi utilise-t-on `rate()` ?
- Comment calculer un percentile ?

---

## 3. Création de Dashboards

### Objectifs
- Créer un dashboard Grafana
- Visualiser les métriques clés
- Configurer des alertes

### Exercice 3.1 : Premier Dashboard

**Tâche** : Créer un dashboard dans Grafana

1. Ouvrez Grafana : http://localhost:3000 (admin/admin)
2. Créez un nouveau dashboard : **+ > Dashboard**
3. Ajoutez un panel avec la requête :

```promql
rate(weather_http_request_duration_seconds_count[5m])
```

4. Configurez :
   - Titre : "Requêtes par seconde - Weather Service"
   - Type de visualisation : Graph
   - Unité : req/s

### Exercice 3.2 : Dashboard Complet

**Tâche** : Créer un dashboard avec 6 panels

Créez les panels suivants :

1. **Taux de requêtes** (tous services)
```promql
sum(rate(weather_http_request_duration_seconds_count[5m])) by (service)
```

2. **Latence P95** (tous services)
```promql
histogram_quantile(0.95, rate(weather_api_latency_seconds_bucket[5m]))
```

3. **Taux d'erreur** (Air Quality)
```promql
rate(airquality_api_errors_total[5m]) / rate(airquality_api_calls_total[5m]) * 100
```

4. **Recommandations par niveau**
```promql
sum by (alert_level) (rate(health_recommendations_total[5m]))
```

5. **Utilisation mémoire** (JVM - Spring Boot)
```promql
jvm_memory_used_bytes{area="heap"}
```

6. **Appels API externes**
```promql
sum by (service, status) (rate(health_api_calls_total[5m]))
```

### Exercice 3.3 : Variables de Dashboard

**Tâche** : Ajouter une variable pour filtrer par service

1. Dashboard Settings > Variables > Add variable
2. Configurez :
   - Name : `service`
   - Type : Query
   - Query : `label_values(service)`
3. Utilisez `$service` dans vos requêtes

---

## 4. Analyse des Performances

### Objectifs
- Identifier les goulots d'étranglement
- Analyser la latence
- Comprendre les percentiles

### Exercice 4.1 : Test de Charge

**Tâche** : Générer de la charge et analyser l'impact

```bash
# Installer 'hey' si nécessaire
# macOS: brew install hey
# Linux: go install github.com/rakyll/hey@latest

# Générer de la charge
hey -n 1000 -c 10 "http://localhost:8081/api/weather/city?city=Paris&country=FR"
```

**Analyses** :
1. Observez dans Grafana :
   - Le taux de requêtes augmente-t-il ?
   - Comment évolue la latence ?
   - Y a-t-il des erreurs ?

2. Calculez les percentiles :
```promql
# P50 (médiane)
histogram_quantile(0.50, rate(weather_api_latency_seconds_bucket[5m]))

# P95
histogram_quantile(0.95, rate(weather_api_latency_seconds_bucket[5m]))

# P99
histogram_quantile(0.99, rate(weather_api_latency_seconds_bucket[5m]))
```

**Questions** :
- Quelle est la différence entre P50 et P95 ?
- Pourquoi P99 est-il important ?
- Quel service est le plus lent ?

### Exercice 4.2 : Analyse de la Chaîne d'Appels

**Tâche** : Analyser les appels en cascade

1. Faites un appel au Health Service :
```bash
curl "http://localhost:8082/api/health/recommendations?city=Paris&country=FR"
```

2. Observez dans les logs les appels en cascade
3. Mesurez la latence de chaque service

**Questions** :
- Combien d'appels sont faits au total ?
- Quel est le temps de réponse total ?
- Comment pourrait-on améliorer les performances ?

---

## 5. Gestion des Incidents

### Objectifs
- Simuler des pannes
- Observer l'impact sur le système
- Apprendre à diagnostiquer

### Exercice 5.1 : Simulation de Panne

**Tâche** : Arrêter le service météo et observer l'impact

```bash
# Arrêter le service
docker-compose stop weather-service

# Attendre 30 secondes

# Essayer d'utiliser l'application
# Observer les métriques dans Prometheus/Grafana
```

**Analyses** :
1. Que se passe-t-il dans le Health Service ?
2. Les erreurs sont-elles visibles dans les métriques ?
3. Comment diagnostiquer rapidement le problème ?

**Résolution** :
```bash
docker-compose start weather-service
```

### Exercice 5.2 : Détection Proactive

**Tâche** : Créer une alerte dans Grafana

1. Dans un panel, cliquez sur **Alert**
2. Configurez une alerte :
   - Condition : Taux d'erreur > 10%
   - Évaluation : toutes les 1 minute

**Questions** :
- Combien de temps faut-il pour détecter une panne ?
- Comment réduire ce temps ?
- Quelles autres métriques devraient déclencher des alertes ?

---

## 6. Métriques Personnalisées

### Objectifs
- Ajouter une nouvelle métrique
- L'exposer dans Prometheus
- La visualiser dans Grafana

### Exercice 6.1 : Ajouter une Métrique (Weather Service)

**Tâche** : Ajouter un counter pour compter les appels par ville

Éditez `weather-service/src/index.js` et ajoutez :

```javascript
const cityRequestCounter = new promClient.Counter({
  name: 'weather_city_requests_total',
  help: 'Total requests by city',
  labelNames: ['city', 'country'],
  registers: [register]
});

// Dans le handler
cityRequestCounter.inc({ city, country });
```

**Vérification** :
```bash
# Redémarrer le service
docker-compose restart weather-service

# Faire quelques requêtes
curl "http://localhost:8081/api/weather/city?city=Paris&country=FR"
curl "http://localhost:8081/api/weather/city?city=London&country=GB"

# Vérifier la métrique
curl http://localhost:8081/metrics | grep weather_city_requests_total
```

**Visualisation** :
```promql
# Top 5 des villes les plus demandées
topk(5, weather_city_requests_total)
```

### Exercice 6.2 : SLI/SLO (Service Level Indicators/Objectives)

**Tâche** : Calculer le SLI de disponibilité

**SLO** : 99.5% de requêtes réussies

```promql
# SLI - Taux de succès sur 5 minutes
(
  sum(rate(weather_http_request_duration_seconds_count{status_code=~"2.."}[5m]))
  /
  sum(rate(weather_http_request_duration_seconds_count[5m]))
) * 100
```

**Questions** :
- Le service respecte-t-il le SLO ?
- Combien d'erreurs peut-on se permettre ?
- Comment améliorer le SLI ?

---

## 7. Exercices Avancés

### Exercice 7.1 : Corrélation de Métriques

**Tâche** : Trouver la corrélation entre latence et charge

1. Générez différents niveaux de charge
2. Observez la latence
3. Créez un scatter plot dans Grafana

### Exercice 7.2 : Budget d'Erreur

**Tâche** : Calculer le budget d'erreur mensuel

Si SLO = 99.5% :
- Temps de downtime autorisé : 0.5%
- Sur 30 jours = 216 minutes (~3.6 heures)

Créez un dashboard montrant :
- Le temps de downtime actuel
- Le budget restant
- Projection sur le mois

### Exercice 7.3 : Optimisation

**Tâche** : Identifier et résoudre un problème de performance

1. Analysez les métriques
2. Identifiez le service le plus lent
3. Proposez 3 solutions d'optimisation
4. Implémentez-en une
5. Mesurez l'amélioration

---

## 📚 Ressources Complémentaires

### Documentation
- [Prometheus Documentation](https://prometheus.io/docs/)
- [PromQL Basics](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Documentation](https://grafana.com/docs/)
- [OpenTelemetry](https://opentelemetry.io/docs/)

### Livres
- "Observability Engineering" - Charity Majors
- "Site Reliability Engineering" - Google
- "Database Reliability Engineering" - Laine Campbell

### Outils
- [PromLens](https://promlens.com/) - Constructeur de requêtes PromQL
- [Robusta](https://home.robusta.dev/) - Alerting pour Kubernetes
- [Grafana Loki](https://grafana.com/oss/loki/) - Log aggregation

---

## ✅ Checklist de Formation

À la fin de cette formation, vous devriez être capable de :

- [ ] Expliquer les 3 piliers de l'observabilité
- [ ] Instrumenter une application avec des métriques
- [ ] Écrire des requêtes PromQL complexes
- [ ] Créer des dashboards Grafana complets
- [ ] Configurer des alertes pertinentes
- [ ] Diagnostiquer des problèmes de performance
- [ ] Définir et mesurer des SLIs/SLOs
- [ ] Analyser des incidents avec les métriques

---

## 🎯 Projet Final

**Mission** : Créer un dashboard de monitoring complet

Votre dashboard doit inclure :
1. **Vue d'ensemble** : Santé globale du système
2. **Performance** : Latence P50/P95/P99 de tous les services
3. **Fiabilité** : Taux d'erreur et SLI
4. **Charge** : Taux de requêtes et tendances
5. **Ressources** : Utilisation CPU/Mémoire
6. **Business** : Métriques métier (villes les plus consultées, etc.)

**Bonus** : Configurez 5 alertes pertinentes

---

Bonne formation ! 🚀
