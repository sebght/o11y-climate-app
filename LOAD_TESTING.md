# 🔥 Load Testing avec k6

## 📊 Configuration Actuelle

Le script k6 génère maintenant une **charge importante sans limite de quotas** car les services utilisent des **données fictives générées localement**.

### Avantages des Données Fictives
- ✅ **Aucune limite de quotas** : Testez autant que vous voulez
- ✅ **Pas de clés API nécessaires** : Démarrage immédiat
- ✅ **Performances constantes** : Pas de dépendance réseau externe
- ✅ **Données cohérentes** : Valeurs réalistes qui varient à chaque requête

### Architecture du Test
Chaque appel au `health-service` déclenche :
- 1 appel à `weather-service` → Génération de données météo fictives
- 1 appel à `air-quality-service` → Génération de données de qualité d'air fictives

## ⚙️ Scénarios de Test

Le script génère une **charge modérée** sur le health-service pour tester tous les services par cascade :

| Scénario | Ville | Taux | Appels/60s |
|----------|-------|------|------------|
| Paris | Paris | 1.67 req/s | 100 |
| Lyon | Lyon | 1.25 req/s | 75 |
| Marseille | Marseille | 0.67 req/s | 40 |
| Toulouse | Toulouse | 0.42 req/s | 25 |
| Nice | Nice | 0.17 req/s | 10 |
| **TOTAL** | - | **4.18 req/s** | **250** |

### Impact Total par Test (60s)
- **250 appels** au health-service
- **250 appels** au weather-service
- **250 appels** à l'air-quality-service
- **750 appels** au total sur l'infrastructure

Cette charge modérée permet de **tester l'observabilité** sans saturer les services, tout en générant suffisamment de métriques et de traces pour analyser le comportement du système.

## 🚀 Utilisation

```bash
# Lancer le test de charge
make test-load

# Ou directement
./scripts/generate-load.sh

# Ou avec k6
k6 run scripts/load-test.js
```

## 📈 Métriques Collectées

Le test k6 mesure automatiquement :
- ✅ Taux de succès des requêtes
- ⏱️ Temps de réponse (P95 < 5s attendu)
- ❌ Taux d'erreur (< 10% attendu)
- 📊 Distribution des latences

## 🔧 Personnalisation

### Modifier la Charge

Éditez `scripts/load-test.js` et ajustez les paramètres. Vous pouvez maintenant augmenter librement la charge :

```javascript
health_paris: {
  executor: 'constant-arrival-rate',
  exec: 'healthParis',
  rate: 60,       // Nombre de requêtes par minute (1 req/s)
  timeUnit: '1m', // Unité de temps
  duration: '60s', // Durée du test
  preAllocatedVUs: 2,
  maxVUs: 10,
}
```

### Ajouter une Ville

1. Créez une nouvelle fonction dans `load-test.js` :
```javascript
export function healthBordeaux() {
  const url = 'http://localhost:8082/api/health/recommendations?city=Bordeaux&country=FR';
  const res = http.get(url);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 5s': (r) => r.timings.duration < 5000,
  });

  errorRate.add(!success);
}
```

2. Ajoutez un scénario dans `options.scenarios` :
```javascript
health_bordeaux: {
  executor: 'constant-arrival-rate',
  exec: 'healthBordeaux',
  rate: 30,  // Pas de limite, ajustez selon vos besoins
  timeUnit: '1m',
  duration: '60s',
  preAllocatedVUs: 1,
  maxVUs: 5,
}
```

### Augmenter la Charge pour Tests de Performance

Vous pouvez maintenant tester les limites de votre infrastructure :

**Charge faible** : 1-5 req/s (monitoring basique)
**Charge moyenne** : 5-20 req/s (test de stabilité)
**Charge élevée** : 20-100 req/s (test de performance)
**Stress test** : 100+ req/s (identifier les limites)

## 📊 Résultats Typiques

Après l'exécution, k6 affiche :

```
scenarios: (100.00%) 5 scenarios, 30 max VUs, 1m30s max duration
  * health_paris: 1.0 iterations/s for 1m0s
  * health_lyon: 0.8 iterations/s for 1m0s
  * health_marseille: 0.5 iterations/s for 1m0s
  * health_toulouse: 0.4 iterations/s for 1m0s
  * health_nice: 0.3 iterations/s for 1m0s

✓ status is 200
✓ response time < 5s

checks.........................: 100.00% ✓ 180  ✗ 0
data_received..................: 450 kB  7.5 kB/s
data_sent......................: 21 kB   350 B/s
http_req_duration..............: avg=0.8s  min=200ms med=0.7s max=1.8s p(95)=1.5s
http_reqs......................: 180     3.0/s
```

## 🎯 Bonnes Pratiques

### Pendant la Formation

1. **Lancez autant de tests que nécessaire** : Plus de quotas à gérer !
2. **Surveillance** : Observez les métriques dans Grafana pendant le test
3. **Expérimentation** : Augmentez progressivement la charge pour trouver les limites
4. **Comparaison** : Lancez plusieurs tests pour comparer les performances

### Optimisation des Tests

```bash
# Lancez des tests successifs pour observer l'évolution
make test-load
sleep 10
make test-load

# Observez l'impact dans Grafana
# http://localhost:3000
```

## 🐛 Troubleshooting

### Tests qui échouent

```bash
# Vérifiez que les services sont UP
docker-compose ps

# Vérifiez les logs
docker-compose logs health-service

# Testez manuellement
curl "http://localhost:8082/api/health/recommendations?city=Paris&country=FR"
```

## 📚 Ressources

- [k6 Documentation](https://k6.io/docs/)
- [k6 Executors](https://k6.io/docs/using-k6/scenarios/executors/)
- [OpenWeather Pricing](https://openweathermap.org/price)
- [OpenAQ API Docs](https://docs.openaq.org/)
