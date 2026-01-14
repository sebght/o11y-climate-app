# 🚀 Démarrage Rapide

Guide de démarrage en 5 minutes pour la plateforme de qualité de l'air.

## Prérequis

- Docker ou Podman installé

**Note:** Les services utilisent des **données fictives générées localement**. Aucune clé API n'est nécessaire.

## Installation en 2 étapes

### 1. Démarrage

```bash
# Lancer tous les services
docker-compose up -d

# Ou avec Make
make start
```

### 2. Vérification

```bash
# Attendre 30-60 secondes pour le démarrage du service Spring Boot
# Puis vérifier que tout fonctionne
make health

# Ou manuellement
curl http://localhost:8080/api/air-quality/health
curl http://localhost:8081/health
curl http://localhost:8082/health
```

## Accès

Ouvrez votre navigateur et accédez à :

| Service | URL | Identifiants |
|---------|-----|--------------|
| **Dashboard** | http://localhost | - |
| **Grafana** | http://localhost:3000 | admin / admin |
| **Prometheus** | http://localhost:9090 | - |

## Premier Test

1. **Ouvrir le dashboard** : http://localhost
2. **Cliquer sur "Paris"** dans les villes populaires
3. **Observer les données** :
   - Qualité de l'air avec AQI
   - Conditions météorologiques
   - Recommandations santé

## Commandes Utiles

```bash
# Voir les logs
docker-compose logs -f

# Voir les logs d'un service spécifique
docker-compose logs -f weather-service

# Arrêter
docker-compose down

# Redémarrer
docker-compose restart

# Nettoyer complètement
docker-compose down -v
```

## Avec Makefile

```bash
make start      # Démarrer
make stop       # Arrêter
make restart    # Redémarrer
make logs       # Voir les logs
make health     # Vérifier la santé
make clean      # Nettoyer
```

## Problèmes Courants

### Les services ne démarrent pas

```bash
# Vérifier les ports
netstat -an | grep LISTEN | grep -E "8080|8081|8082"

# Si des ports sont occupés, les libérer ou modifier docker-compose.yml
```

### Le service Spring Boot est très lent

C'est normal pour le premier démarrage (build Maven). Attendez 60 secondes.

### "Cannot connect to service"

```bash
# Vérifier que tous les conteneurs tournent
docker-compose ps

# Redémarrer les services
docker-compose restart
```

## Prochaines Étapes

1. **Explorer les métriques** dans Prometheus : http://localhost:9090
2. **Créer des dashboards** dans Grafana : http://localhost:3000
3. **Consulter les exercices** dans TRAINING.md
4. **Lire la documentation complète** dans README.md

## Support

Pour plus de détails, consultez le [README.md](README.md) complet.
