import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Métriques personnalisées
const errorRate = new Rate('errors');

// Configuration des scénarios de charge
// ⚠️ IMPORTANT: Taux réduit pour respecter les quotas API du Free tier
// - OpenWeather: 1000 req/jour
// - OpenAQ: 60 req/minute
// Chaque appel à health-service appelle weather + air-quality
export const options = {
  scenarios: {
    health_paris: {
      executor: 'constant-arrival-rate',
      exec: 'healthParis',
      rate: 4,
      timeUnit: '1m',
      duration: '60s',
      preAllocatedVUs: 1,
      maxVUs: 3,
    },
    health_lyon: {
      executor: 'constant-arrival-rate',
      exec: 'healthLyon',
      rate: 2,
      timeUnit: '1m',
      duration: '60s',
      preAllocatedVUs: 1,
      maxVUs: 3,
    },
    health_marseille: {
      executor: 'constant-arrival-rate',
      exec: 'healthMarseille',
      rate: 1,
      timeUnit: '1m',
      duration: '60s',
      preAllocatedVUs: 1,
      maxVUs: 2,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    errors: ['rate<0.1'],
  },
};

// Fonction pour Health Service - Paris
export function healthParis() {
  const url = 'http://localhost:8082/api/health/recommendations?city=Paris&country=FR';
  const res = http.get(url);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 5s': (r) => r.timings.duration < 5000,
  });

  errorRate.add(!success);
}

// Fonction pour Health Service - Lyon
export function healthLyon() {
  const url = 'http://localhost:8082/api/health/recommendations?city=Lyon&country=FR';
  const res = http.get(url);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 5s': (r) => r.timings.duration < 5000,
  });

  errorRate.add(!success);
}

// Fonction pour Health Service - Marseille
export function healthMarseille() {
  const url = 'http://localhost:8082/api/health/recommendations?city=Marseille&country=FR';
  const res = http.get(url);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 5s': (r) => r.timings.duration < 5000,
  });

  errorRate.add(!success);
}
