# 🔥 Load Testing avec k6

## 📊 Configuration Actuelle

Le script k6 est configuré pour **respecter les quotas des API externes** :

### Quotas API
- **OpenWeather**: 1000 requêtes/jour (free tier)
- **OpenAQ**: 60 requêtes/minute ou 2000 requêtes/heure (free tier)

### Impact du Health Service
Chaque appel au `health-service` déclenche :
- 1 appel à `weather-service` → OpenWeather API
- 1 appel à `air-quality-service` → OpenAQ API

## ⚙️ Scénarios de Test

Le script génère une charge **uniquement sur le health-service** pour tester tous les services par ruissellement :

| Scénario | Ville | Taux | Appels/60s |
|----------|-------|------|------------|
| Paris | Paris | 0.15 req/s | 9 |
| Lyon | Lyon | 0.10 req/s | 6 |
| Marseille | Marseille | 0.05 req/s | 3 |
| **TOTAL** | - | **0.3 req/s** | **18** |

### Calcul des Quotas

**Par exécution (60s)** :
- 18 appels au health-service
- → 18 appels à OpenWeather
- → 18 appels à OpenAQ

**Lancements possibles par jour** :
- OpenWeather : 1000 ÷ 18 = **~55 exécutions/jour**
- OpenAQ : 2000 ÷ 18 = **~111 exécutions/jour**

**Quota limitant** : OpenWeather avec ~55 lancements maximum par jour

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

Éditez `scripts/load-test.js` et ajustez les paramètres :

```javascript
health_paris: {
  executor: 'constant-arrival-rate',
  exec: 'healthParis',
  rate: 9,        // Nombre de requêtes par minute
  timeUnit: '1m', // Unité de temps
  duration: '60s', // Durée du test
  preAllocatedVUs: 1,
  maxVUs: 3,
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
  rate: 3,
  timeUnit: '1m',
  duration: '60s',
  preAllocatedVUs: 1,
  maxVUs: 2,
}
```

⚠️ **Attention** : Ajustez les taux pour rester sous les quotas !

### Augmenter la Charge

Si vous avez un plan payant avec des quotas plus élevés :

1. Calculez votre quota disponible
2. Divisez par le nombre d'exécutions souhaitées par jour
3. Ajustez les `rate` en conséquence

**Exemple** : Quota OpenWeather de 10,000/jour
- Pour 50 exécutions/jour : 10,000 ÷ 50 = 200 appels par test
- Durée 60s : 200 ÷ 60 = 3.33 req/s
- Ajustez : `rate: 200` avec `timeUnit: '1m'`

## 📊 Résultats Typiques

Après l'exécution, k6 affiche :

```
scenarios: (100.00%) 3 scenarios, 8 max VUs, 1m30s max duration
  * health_paris: 0.15 iterations/s for 1m0s
  * health_lyon: 0.10 iterations/s for 1m0s
  * health_marseille: 0.05 iterations/s for 1m0s

✓ status is 200
✓ response time < 5s

checks.........................: 100.00% ✓ 18  ✗ 0
data_received..................: 45 kB   750 B/s
data_sent......................: 2.1 kB  35 B/s
http_req_duration..............: avg=1.2s  min=450ms med=1.1s max=2.3s p(95)=2.1s
http_reqs......................: 18      0.3/s
```

## 🎯 Bonnes Pratiques

### Pendant la Formation

1. **Avant chaque session** : Vérifiez vos quotas restants
2. **Entre les tests** : Attendez quelques minutes pour étaler la charge
3. **Surveillance** : Observez les métriques dans Grafana pendant le test
4. **Documentation** : Notez combien de fois vous avez lancé le test

### Gestion des Quotas

```bash
# Comptez vos exécutions de la journée
echo "Exécutions aujourd'hui: X/55"

# Si vous approchez de la limite, réduisez la durée
# Éditez load-test.js et changez duration: '60s' → '30s'
```

## 🐛 Troubleshooting

### Erreur 429 (Too Many Requests)

Vous avez atteint votre quota API :
- Attendez la réinitialisation (minuit UTC pour OpenWeather)
- Vérifiez votre dashboard API
- Réduisez temporairement la charge

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
