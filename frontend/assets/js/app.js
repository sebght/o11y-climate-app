// Configuration
const API_BASE_URL = window.location.hostname === 'localhost'
    ? 'http://localhost'
    : window.location.origin;

const SERVICES = {
    airQuality: `${API_BASE_URL}:8080`,
    weather: `${API_BASE_URL}:8081`,
    health: `${API_BASE_URL}:8082`
};

// État de l'application
let map;
let currentMarker;
let selectedCity = null;

// Initialisation
document.addEventListener('DOMContentLoaded', () => {
    initMap();
    setupEventListeners();
    checkServicesStatus();
    setInterval(checkServicesStatus, 30000); // Vérifier toutes les 30s
});

// Initialisation de la carte Leaflet
function initMap() {
    map = L.map('map').setView([48.8566, 2.3522], 5); // Paris par défaut

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        maxZoom: 18
    }).addTo(map);

    // Événement de clic sur la carte
    map.on('click', async (e) => {
        const { lat, lng } = e.latlng;
        await fetchDataByCoordinates(lat, lng);
    });
}

// Configuration des écouteurs d'événements
function setupEventListeners() {
    document.getElementById('searchBtn').addEventListener('click', searchCity);
    document.getElementById('cityInput').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') searchCity();
    });

    document.querySelectorAll('.city-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const city = e.target.dataset.city;
            const country = e.target.dataset.country;
            document.getElementById('cityInput').value = city;
            fetchDataByCity(city, country);
        });
    });
}

// Recherche de ville
async function searchCity() {
    const cityInput = document.getElementById('cityInput').value.trim();
    if (!cityInput) {
        showToast('Veuillez entrer un nom de ville', 'error');
        return;
    }

    await fetchDataByCity(cityInput);
}

// Récupération des données par ville
async function fetchDataByCity(city, country = 'FR') {
    selectedCity = { city, country };
    showLoading(true);

    try {
        // Appels parallèles aux 3 services
        const [airQualityData, weatherData, healthData] = await Promise.all([
            fetchAirQuality(city, country),
            fetchWeather(city, country),
            fetchHealthRecommendations(city, country)
        ]);

        // Mise à jour de l'interface
        displayAirQuality(airQualityData);
        displayWeather(weatherData);
        displayHealthRecommendations(healthData);

        // Mise à jour de la carte
        if (weatherData && weatherData.latitude && weatherData.longitude) {
            updateMapLocation(weatherData.latitude, weatherData.longitude, city);
        }

        showToast(`Données chargées pour ${city}`, 'success');
    } catch (error) {
        console.error('Error fetching data:', error);
        showToast('Erreur lors du chargement des données', 'error');
    } finally {
        showLoading(false);
    }
}

// Récupération des données par coordonnées
async function fetchDataByCoordinates(lat, lng) {
    showLoading(true);

    try {
        const [airQualityData, weatherData] = await Promise.all([
            fetchAirQualityByCoordinates(lat, lng),
            fetchWeatherByCoordinates(lat, lng)
        ]);

        if (weatherData && weatherData.city) {
            const healthData = await fetchHealthRecommendations(weatherData.city, weatherData.country);
            displayHealthRecommendations(healthData);
        }

        displayAirQuality(airQualityData);
        displayWeather(weatherData);

        updateMapLocation(lat, lng, weatherData?.city || 'Location');
        showToast('Données chargées pour cette localisation', 'success');
    } catch (error) {
        console.error('Error fetching data by coordinates:', error);
        showToast('Erreur lors du chargement des données', 'error');
    } finally {
        showLoading(false);
    }
}

// Appels API
async function fetchAirQuality(city, country) {
    const response = await fetch(`${SERVICES.airQuality}/api/air-quality/city?city=${encodeURIComponent(city)}&country=${country}`);
    if (!response.ok) throw new Error('Air quality data fetch failed');
    return await response.json();
}

async function fetchAirQualityByCoordinates(lat, lng) {
    const response = await fetch(`${SERVICES.airQuality}/api/air-quality/coordinates?latitude=${lat}&longitude=${lng}&radius=25000`);
    if (!response.ok) throw new Error('Air quality data fetch failed');
    return await response.json();
}

async function fetchWeather(city, country) {
    const response = await fetch(`${SERVICES.weather}/api/weather/city?city=${encodeURIComponent(city)}&country=${country}`);
    if (!response.ok) throw new Error('Weather data fetch failed');
    return await response.json();
}

async function fetchWeatherByCoordinates(lat, lng) {
    const response = await fetch(`${SERVICES.weather}/api/weather/coordinates?latitude=${lat}&longitude=${lng}`);
    if (!response.ok) throw new Error('Weather data fetch failed');
    return await response.json();
}

async function fetchHealthRecommendations(city, country) {
    const response = await fetch(`${SERVICES.health}/api/health/recommendations?city=${encodeURIComponent(city)}&country=${country}`);
    if (!response.ok) throw new Error('Health recommendations fetch failed');
    return await response.json();
}

// Affichage des données - Qualité de l'air
function displayAirQuality(data) {
    const card = document.getElementById('airQualityCard');
    const content = card.querySelector('.air-quality-content');
    const loading = card.querySelector('.loading');

    loading.style.display = 'none';
    content.style.display = 'block';

    if (!data || data.length === 0) {
        content.innerHTML = '<p>Aucune donnée disponible</p>';
        return;
    }

    // Calculer l'AQI moyen
    const avgAqi = Math.round(data.reduce((sum, item) => sum + (item.aqi || 0), 0) / data.length);
    const qualityLevel = data[0].qualityLevel || 'Unknown';

    // Mettre à jour le badge AQI
    const aqiValue = document.getElementById('aqiValue');
    const aqiBadge = aqiValue.closest('.aqi-badge');
    aqiValue.textContent = avgAqi;

    // Couleur selon l'AQI
    aqiBadge.className = 'aqi-badge ' + getAqiClass(avgAqi);

    // Niveau de qualité
    document.getElementById('qualityLevel').textContent = qualityLevel;
    document.getElementById('qualityLevel').className = 'quality-level ' + getAqiClass(avgAqi);

    // Mesures
    const measurementsDiv = document.getElementById('measurements');
    measurementsDiv.innerHTML = data.slice(0, 5).map(item => `
        <div class="measurement-item">
            <span><strong>${item.parameter.toUpperCase()}</strong></span>
            <span>${item.value.toFixed(2)} ${item.unit}</span>
        </div>
    `).join('');
}

// Affichage des données - Météo
function displayWeather(data) {
    const card = document.getElementById('weatherCard');
    const content = card.querySelector('.weather-content');
    const loading = card.querySelector('.loading');

    loading.style.display = 'none';
    content.style.display = 'block';

    if (!data) {
        content.innerHTML = '<p>Aucune donnée disponible</p>';
        return;
    }

    document.getElementById('temperature').textContent = `${Math.round(data.temperature)}°C`;
    document.getElementById('weatherDesc').textContent = data.description;

    const detailsDiv = document.getElementById('weatherDetails');
    detailsDiv.innerHTML = `
        <div class="weather-item">
            <span>💧 Humidité</span>
            <span>${data.humidity}%</span>
        </div>
        <div class="weather-item">
            <span>🌡️ Ressenti</span>
            <span>${Math.round(data.feelsLike)}°C</span>
        </div>
        <div class="weather-item">
            <span>💨 Vent</span>
            <span>${data.windSpeed} m/s</span>
        </div>
        <div class="weather-item">
            <span>☁️ Nuages</span>
            <span>${data.clouds}%</span>
        </div>
        <div class="weather-item">
            <span>👁️ Visibilité</span>
            <span>${(data.visibility / 1000).toFixed(1)} km</span>
        </div>
    `;
}

// Affichage des données - Recommandations santé
function displayHealthRecommendations(data) {
    const card = document.getElementById('healthCard');
    const content = card.querySelector('.health-content');
    const loading = card.querySelector('.loading');

    loading.style.display = 'none';
    content.style.display = 'block';

    if (!data) {
        content.innerHTML = '<p>Aucune donnée disponible</p>';
        return;
    }

    // Badge d'alerte
    const alertBadge = document.getElementById('alertBadge');
    const alertLevel = document.getElementById('alertLevel');
    alertLevel.textContent = getAlertLevelText(data.alert_level);
    alertBadge.className = 'alert-badge alert-' + data.alert_level;

    // Recommandations
    const recommendationsDiv = document.getElementById('recommendations');
    recommendationsDiv.innerHTML = data.recommendations.map(rec => `
        <div class="recommendation-item">${rec}</div>
    `).join('');

    // Groupes à risque
    const riskGroupsDiv = document.getElementById('riskGroups');
    if (data.at_risk_groups.length > 0) {
        riskGroupsDiv.innerHTML = `
            <h4>⚠️ Groupes à risque:</h4>
            <ul>${data.at_risk_groups.map(group => `<li>${group}</li>`).join('')}</ul>
        `;
        riskGroupsDiv.style.display = 'block';
    } else {
        riskGroupsDiv.style.display = 'none';
    }

    // Activités suggérées
    const activitiesDiv = document.getElementById('activities');
    if (data.suggested_activities.length > 0) {
        activitiesDiv.innerHTML = `
            <h4>✅ Activités suggérées:</h4>
            <ul>${data.suggested_activities.map(activity => `<li>${activity}</li>`).join('')}</ul>
        `;
        activitiesDiv.style.display = 'block';
    } else {
        activitiesDiv.style.display = 'none';
    }
}

// Mise à jour de la carte
function updateMapLocation(lat, lng, name) {
    if (currentMarker) {
        map.removeLayer(currentMarker);
    }

    currentMarker = L.marker([lat, lng]).addTo(map);
    currentMarker.bindPopup(`<b>${name}</b>`).openPopup();
    map.setView([lat, lng], 10);
}

// Vérification du statut des services
async function checkServicesStatus() {
    const services = [
        { id: 'airQualityStatus', url: `${SERVICES.airQuality}/api/air-quality/health` },
        { id: 'weatherStatus', url: `${SERVICES.weather}/health` },
        { id: 'healthStatus', url: `${SERVICES.health}/health` }
    ];

    for (const service of services) {
        try {
            const response = await fetch(service.url, { timeout: 5000 });
            const statusDot = document.querySelector(`#${service.id} .status-dot`);
            if (response.ok) {
                statusDot.className = 'status-dot status-ok';
            } else {
                statusDot.className = 'status-dot status-error';
            }
        } catch (error) {
            const statusDot = document.querySelector(`#${service.id} .status-dot`);
            statusDot.className = 'status-dot status-error';
        }
    }
}

// Utilitaires
function getAqiClass(aqi) {
    if (aqi <= 50) return 'aqi-good';
    if (aqi <= 100) return 'aqi-moderate';
    if (aqi <= 150) return 'aqi-unhealthy-sensitive';
    if (aqi <= 200) return 'aqi-unhealthy';
    if (aqi <= 300) return 'aqi-very-unhealthy';
    return 'aqi-hazardous';
}

function getAlertLevelText(level) {
    const levels = {
        'low': 'Faible',
        'moderate': 'Modéré',
        'high': 'Élevé',
        'very_high': 'Très Élevé',
        'extreme': 'Extrême'
    };
    return levels[level] || level;
}

function showLoading(show) {
    const cards = ['airQualityCard', 'weatherCard', 'healthCard'];
    cards.forEach(cardId => {
        const card = document.getElementById(cardId);
        const loading = card.querySelector('.loading');
        const content = card.querySelector('.air-quality-content, .weather-content, .health-content');

        if (show) {
            loading.style.display = 'block';
            if (content) content.style.display = 'none';
        }
    });
}

function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type} show`;

    setTimeout(() => {
        toast.className = 'toast';
    }, 3000);
}
