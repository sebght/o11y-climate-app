#!/bin/bash

# Script d'initialisation du fichier .env
# Usage: ./scripts/init-env.sh

set -e

echo "🔧 Initialisation du fichier .env..."
echo ""

# Vérifier si .env existe déjà
if [ -f .env ]; then
    echo "⚠️  Le fichier .env existe déjà."
    read -p "Voulez-vous le remplacer ? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Opération annulée"
        exit 0
    fi
fi

# Demander la clé API OpenWeatherMap
echo "📝 Configuration de l'API OpenWeatherMap"
echo ""
echo "Pour obtenir une clé API gratuite:"
echo "  1. Allez sur https://openweathermap.org/api"
echo "  2. Créez un compte gratuit"
echo "  3. Générez une clé API (Free tier: 60 appels/minute)"
echo ""
read -p "Entrez votre clé API OpenWeatherMap (ou laissez vide pour 'demo'): " api_key

if [ -z "$api_key" ]; then
    api_key="demo"
    echo "⚠️  Mode démo activé (fonctionnalités limitées)"
fi

# Créer le fichier .env
cat > .env << EOF
# API Keys
OPENWEATHER_API_KEY=${api_key}

# Ports (par défaut)
AIR_QUALITY_PORT=8080
WEATHER_PORT=8081
HEALTH_PORT=8082
FRONTEND_PORT=80
PROMETHEUS_PORT=9090
GRAFANA_PORT=3000

# Grafana
GF_SECURITY_ADMIN_USER=admin
GF_SECURITY_ADMIN_PASSWORD=admin
EOF

echo ""
echo "✅ Fichier .env créé avec succès!"
echo ""

# Afficher un résumé
echo "📋 Configuration:"
echo "  - OpenWeatherMap API Key: ${api_key}"
echo "  - Grafana User: admin"
echo "  - Grafana Password: admin"
echo ""

# Proposer de démarrer les services
read -p "Voulez-vous démarrer les services maintenant ? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🚀 Démarrage des services..."
    docker-compose up -d
    echo ""
    echo "✅ Services démarrés!"
    echo ""
    echo "📊 Accès aux services:"
    echo "  - Dashboard: http://localhost"
    echo "  - Grafana: http://localhost:3000"
    echo "  - Prometheus: http://localhost:9090"
else
    echo ""
    echo "Pour démarrer les services plus tard, exécutez:"
    echo "  docker-compose up -d"
    echo ""
    echo "ou:"
    echo "  make start"
fi
