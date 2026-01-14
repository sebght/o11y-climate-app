import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Métriques personnalisées
const errorRate = new Rate('errors');

// Configuration des scénarios de charge
// Les services utilisent des données fictives, donc pas de limite de quotas !
// Chaque appel à health-service appelle weather + air-quality (données générées localement)
// Target: 250 requêtes en 60 secondes = ~4.2 req/s (charge modérée et soutenable)
export const options = {
  scenarios: {
    health_paris: {
      executor: 'constant-arrival-rate',
      exec: 'healthParis',
      rate: 100,  // 1.67 req/s
      timeUnit: '1m',
      duration: '60s',
      preAllocatedVUs: 3,
      maxVUs: 15,
    },
    health_lyon: {
      executor: 'constant-arrival-rate',
      exec: 'healthLyon',
      rate: 75,  // 1.25 req/s
      timeUnit: '1m',
      duration: '60s',
      preAllocatedVUs: 2,
      maxVUs: 12,
    },
    health_marseille: {
      executor: 'constant-arrival-rate',
      exec: 'healthMarseille',
      rate: 40,  // 0.67 req/s
      timeUnit: '1m',
      duration: '60s',
      preAllocatedVUs: 2,
      maxVUs: 8,
    },
    health_toulouse: {
      executor: 'constant-arrival-rate',
      exec: 'healthToulouse',
      rate: 25,  // 0.42 req/s
      timeUnit: '1m',
      duration: '60s',
      preAllocatedVUs: 1,
      maxVUs: 6,
    },
    health_nice: {
      executor: 'constant-arrival-rate',
      exec: 'healthNice',
      rate: 10,  // 0.17 req/s
      timeUnit: '1m',
      duration: '60s',
      preAllocatedVUs: 1,
      maxVUs: 4,
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

// Fonction pour Health Service - Toulouse
export function healthToulouse() {
  const url = 'http://localhost:8082/api/health/recommendations?city=Toulouse&country=FR';
  const res = http.get(url);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 5s': (r) => r.timings.duration < 5000,
  });

  errorRate.add(!success);
}

// Fonction pour Health Service - Nice
export function healthNice() {
  const url = 'http://localhost:8082/api/health/recommendations?city=Nice&country=FR';
  const res = http.get(url);

  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 5s': (r) => r.timings.duration < 5000,
  });

  errorRate.add(!success);
}
