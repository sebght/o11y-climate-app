#!/bin/bash

# Script d'installation et de configuration
# Usage: ./scripts/setup.sh

set -e

echo "🚀 Configuration de l'environnement..."
echo ""

# Vérifier Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker n'est pas installé. Veuillez l'installer d'abord."
    exit 1
fi

echo "✅ Docker trouvé: $(docker --version)"

# Vérifier Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose n'est pas installé. Veuillez l'installer d'abord."
    exit 1
fi

echo "✅ Docker Compose trouvé: $(docker-compose --version)"

# Créer le fichier .env s'il n'existe pas
if [ ! -f .env ]; then
    echo "📝 Création du fichier .env..."
    cp .env.example .env
    echo "⚠️  N'oubliez pas de configurer votre clé API OpenWeatherMap dans le fichier .env"
else
    echo "✅ Fichier .env existe déjà"
fi

# Créer les dossiers nécessaires
echo "📁 Création des dossiers..."
mkdir -p observability/grafana/provisioning/{dashboards,datasources}

# Rendre les scripts exécutables
echo "🔧 Configuration des permissions..."
chmod +x scripts/*.sh

echo ""
echo "✅ Configuration terminée!"
echo ""
echo "📖 Prochaines étapes:"
echo "  1. Éditez le fichier .env et ajoutez votre clé API OpenWeatherMap"
echo "  2. Lancez les services: make start"
echo "  3. Ouvrez http://localhost dans votre navigateur"
echo ""
echo "Pour plus d'informations, consultez le README.md"
