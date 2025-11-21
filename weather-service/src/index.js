// IMPORTANT: Charger le tracing en PREMIER avant tout autre module
require('./tracing');

const express = require('express');
const cors = require('cors');
const axios = require('axios');
const promClient = require('prom-client');

const app = express();
const PORT = process.env.PORT || 8081;
const OPENWEATHER_API_KEY = process.env.OPENWEATHER_API_KEY || 'demo';

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
  help: 'Total number of calls to OpenWeatherMap API',
  labelNames: ['endpoint', 'status'],
  registers: [register]
});

const apiLatencyHistogram = new promClient.Histogram({
  name: 'weather_api_latency_seconds',
  help: 'Latency of OpenWeatherMap API calls',
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

    const response = await axios.get('https://api.openweathermap.org/data/2.5/weather', {
      params: {
        q: `${city},${country}`,
        appid: OPENWEATHER_API_KEY,
        units: 'metric',
        lang: 'fr'
      },
      timeout: 10000
    });

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

    const response = await axios.get('https://api.openweathermap.org/data/2.5/weather', {
      params: {
        lat: latitude,
        lon: longitude,
        appid: OPENWEATHER_API_KEY,
        units: 'metric',
        lang: 'fr'
      },
      timeout: 10000
    });

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

    const response = await axios.get('https://api.openweathermap.org/data/2.5/forecast', {
      params: {
        q: `${city},${country}`,
        appid: OPENWEATHER_API_KEY,
        units: 'metric',
        lang: 'fr'
      },
      timeout: 10000
    });

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
