// IMPORTANT: Charger le tracing en PREMIER avant tout autre module
require('./tracing');

const express = require('express');
const cors = require('cors');
const promClient = require('prom-client');

const app = express();
const PORT = process.env.PORT || 8081;

// Middleware
app.use(cors());
app.use(express.json());

// Prometheus metrics
const register = new promClient.Registry();
promClient.collectDefaultMetrics({ register });

// Métriques personnalisées
const httpRequestDuration = new promClient.Histogram({
  name: 'weather_http_request_duration_seconds',
  help: 'Duration of HTTP requests in seconds',
  labelNames: ['method', 'route', 'status_code'],
  registers: [register]
});

const apiCallCounter = new promClient.Counter({
  name: 'weather_api_calls_total',
  help: 'Total number of weather data requests',
  labelNames: ['endpoint', 'status'],
  registers: [register]
});

const apiLatencyHistogram = new promClient.Histogram({
  name: 'weather_api_latency_seconds',
  help: 'Latency of weather data generation',
  labelNames: ['endpoint'],
  buckets: [0.1, 0.3, 0.5, 0.7, 1, 2, 5],
  registers: [register]
});

// Middleware pour mesurer la durée des requêtes
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = (Date.now() - start) / 1000;
    httpRequestDuration.labels(req.method, req.route?.path || req.path, res.statusCode).observe(duration);
  });
  next();
});

// Fonction pour simuler de la latence variable
function simulateLatency() {
  const delay = Math.floor(Math.random() * 300) + 100; // 100-400ms
  return new Promise(resolve => setTimeout(resolve, delay));
}

// Générateur de données météo fictives
function generateMockWeatherData(city, country = 'FR') {
  // Coordonnées approximatives des villes (pour la cohérence)
  const cityCoordinates = {
    'Paris': { lat: 48.8566, lon: 2.3522 },
    'Lyon': { lat: 45.7640, lon: 4.8357 },
    'Marseille': { lat: 43.2965, lon: 5.3698 },
    'Toulouse': { lat: 43.6047, lon: 1.4442 },
    'Nice': { lat: 43.7102, lon: 7.2620 },
    'Nantes': { lat: 47.2184, lon: -1.5536 },
    'Strasbourg': { lat: 48.5734, lon: 7.7521 },
    'Bordeaux': { lat: 44.8378, lon: -0.5792 },
    'Lille': { lat: 50.6292, lon: 3.0573 },
    'Rennes': { lat: 48.1173, lon: -1.6778 }
  };

  const coords = cityCoordinates[city] || cityCoordinates['Paris'];

  // Température de base pour janvier (hivernale)
  const baseTemp = 5 + Math.random() * 5; // 5-10°C
  const variation = (Math.random() - 0.5) * 4; // Variation de -2 à +2°C
  const temperature = Math.round((baseTemp + variation) * 10) / 10;

  // Conditions météo d'hiver
  const winterConditions = [
    { desc: 'ciel dégagé', icon: '01d', clouds: 10 },
    { desc: 'peu nuageux', icon: '02d', clouds: 25 },
    { desc: 'partiellement nuageux', icon: '03d', clouds: 50 },
    { desc: 'couvert', icon: '04d', clouds: 90 },
    { desc: 'petite pluie', icon: '10d', clouds: 85 },
    { desc: 'pluie', icon: '10d', clouds: 95 },
    { desc: 'brouillard', icon: '50d', clouds: 100 }
  ];

  const condition = winterConditions[Math.floor(Math.random() * winterConditions.length)];

  return {
    name: city,
    sys: { country: country },
    coord: coords,
    main: {
      temp: temperature,
      feels_like: temperature - (Math.random() * 2 + 1),
      humidity: 60 + Math.floor(Math.random() * 30), // 60-90% en hiver
      pressure: 1010 + Math.floor(Math.random() * 20) // 1010-1030 hPa
    },
    weather: [{
      description: condition.desc,
      icon: condition.icon
    }],
    wind: {
      speed: Math.random() * 8 + 2, // 2-10 m/s
      deg: Math.floor(Math.random() * 360)
    },
    clouds: {
      all: condition.clouds + Math.floor(Math.random() * 10)
    },
    visibility: Math.floor(Math.random() * 5000) + 5000, // 5-10km
    dt: Math.floor(Date.now() / 1000)
  };
}

// Générateur de prévisions météo fictives
function generateMockForecast(city, country = 'FR', days = 5) {
  const baseTemp = 5 + Math.random() * 5;
  const forecastItems = [];

  const conditions = [
    { desc: 'ciel dégagé', icon: '01d' },
    { desc: 'peu nuageux', icon: '02d' },
    { desc: 'couvert', icon: '04d' },
    { desc: 'pluie légère', icon: '10d' },
    { desc: 'pluie', icon: '10d' }
  ];

  // Générer des prévisions toutes les 3 heures pendant N jours
  for (let i = 0; i < days * 8; i++) {
    const hourOffset = i * 3;
    const tempVariation = Math.sin(hourOffset / 12) * 3; // Variation jour/nuit
    const randomVariation = (Math.random() - 0.5) * 2;
    const temp = Math.round((baseTemp + tempVariation + randomVariation) * 10) / 10;

    const condition = conditions[Math.floor(Math.random() * conditions.length)];

    forecastItems.push({
      dt: Math.floor(Date.now() / 1000) + (hourOffset * 3600),
      main: {
        temp: temp,
        humidity: 60 + Math.floor(Math.random() * 30)
      },
      weather: [{
        description: condition.desc,
        icon: condition.icon
      }],
      wind: {
        speed: Math.random() * 8 + 2
      }
    });
  }

  return {
    city: {
      name: city,
      country: country
    },
    list: forecastItems
  };
}

// Routes
app.get('/api/weather/city', async (req, res) => {
  const { city, country = 'FR' } = req.query;

  if (!city) {
    return res.status(400).json({ error: 'City parameter is required' });
  }

  console.log(`[Weather] Fetching weather for city: ${city}, country: ${country}`);
  const endTimer = apiLatencyHistogram.startTimer({ endpoint: 'current' });

  try {
    await simulateLatency();

    // Utilisation des données fictives au lieu de l'API réelle
    const response = { data: generateMockWeatherData(city, country) };

    apiCallCounter.inc({ endpoint: 'current', status: 'success' });
    endTimer();

    const weatherData = {
      city: response.data.name,
      country: response.data.sys.country,
      temperature: response.data.main.temp,
      feelsLike: response.data.main.feels_like,
      humidity: response.data.main.humidity,
      pressure: response.data.main.pressure,
      description: response.data.weather[0].description,
      icon: response.data.weather[0].icon,
      windSpeed: response.data.wind.speed,
      windDirection: response.data.wind.deg,
      clouds: response.data.clouds.all,
      visibility: response.data.visibility,
      timestamp: new Date(response.data.dt * 1000).toISOString()
    };

    res.json(weatherData);
  } catch (error) {
    console.error('[Weather] Error fetching weather data:', error.message);
    apiCallCounter.inc({ endpoint: 'current', status: 'error' });
    endTimer();

    if (error.response?.status === 401) {
      return res.status(503).json({
        error: 'Weather API key not configured or invalid',
        message: 'Please set OPENWEATHER_API_KEY environment variable'
      });
    }

    res.status(500).json({
      error: 'Error fetching weather data',
      message: error.message
    });
  }
});

app.get('/api/weather/coordinates', async (req, res) => {
  const { latitude, longitude } = req.query;

  if (!latitude || !longitude) {
    return res.status(400).json({ error: 'Latitude and longitude parameters are required' });
  }

  console.log(`[Weather] Fetching weather for coordinates: ${latitude}, ${longitude}`);
  const endTimer = apiLatencyHistogram.startTimer({ endpoint: 'current' });

  try {
    await simulateLatency();

    // Utilisation des données fictives au lieu de l'API réelle
    const mockData = generateMockWeatherData('Unknown');
    mockData.coord = { lat: parseFloat(latitude), lon: parseFloat(longitude) };
    const response = { data: mockData };

    apiCallCounter.inc({ endpoint: 'current', status: 'success' });
    endTimer();

    const weatherData = {
      city: response.data.name,
      country: response.data.sys.country,
      latitude: response.data.coord.lat,
      longitude: response.data.coord.lon,
      temperature: response.data.main.temp,
      feelsLike: response.data.main.feels_like,
      humidity: response.data.main.humidity,
      pressure: response.data.main.pressure,
      description: response.data.weather[0].description,
      icon: response.data.weather[0].icon,
      windSpeed: response.data.wind.speed,
      windDirection: response.data.wind.deg,
      clouds: response.data.clouds.all,
      visibility: response.data.visibility,
      timestamp: new Date(response.data.dt * 1000).toISOString()
    };

    res.json(weatherData);
  } catch (error) {
    console.error('[Weather] Error fetching weather data:', error.message);
    apiCallCounter.inc({ endpoint: 'current', status: 'error' });
    endTimer();

    if (error.response?.status === 401) {
      return res.status(503).json({
        error: 'Weather API key not configured or invalid',
        message: 'Please set OPENWEATHER_API_KEY environment variable'
      });
    }

    res.status(500).json({
      error: 'Error fetching weather data',
      message: error.message
    });
  }
});

app.get('/api/weather/forecast', async (req, res) => {
  const { city, country = 'FR', days = 5 } = req.query;

  if (!city) {
    return res.status(400).json({ error: 'City parameter is required' });
  }

  console.log(`[Weather] Fetching ${days}-day forecast for city: ${city}`);
  const endTimer = apiLatencyHistogram.startTimer({ endpoint: 'forecast' });

  try {
    await simulateLatency();

    // Utilisation des données fictives au lieu de l'API réelle
    const response = { data: generateMockForecast(city, country, days) };

    apiCallCounter.inc({ endpoint: 'forecast', status: 'success' });
    endTimer();

    const forecast = response.data.list.slice(0, days * 8).map(item => ({
      timestamp: new Date(item.dt * 1000).toISOString(),
      temperature: item.main.temp,
      description: item.weather[0].description,
      icon: item.weather[0].icon,
      humidity: item.main.humidity,
      windSpeed: item.wind.speed
    }));

    res.json({
      city: response.data.city.name,
      country: response.data.city.country,
      forecast
    });
  } catch (error) {
    console.error('[Weather] Error fetching forecast data:', error.message);
    apiCallCounter.inc({ endpoint: 'forecast', status: 'error' });
    endTimer();

    if (error.response?.status === 401) {
      return res.status(503).json({
        error: 'Weather API key not configured or invalid',
        message: 'Please set OPENWEATHER_API_KEY environment variable'
      });
    }

    res.status(500).json({
      error: 'Error fetching forecast data',
      message: error.message
    });
  }
});

// Endpoints de monitoring
app.get('/metrics', async (req, res) => {
  res.set('Content-Type', register.contentType);
  res.end(await register.metrics());
});

app.get('/health', (req, res) => {
  res.json({ status: 'OK', service: 'weather-service' });
});

// Start server
app.listen(PORT, () => {
  console.log(`[Weather] Service started on port ${PORT}`);
  console.log(`[Weather] Metrics available at http://localhost:${PORT}/metrics`);
  console.log(`[Weather] Health check at http://localhost:${PORT}/health`);
});
