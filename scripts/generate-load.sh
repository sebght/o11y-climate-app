#!/bin/bash

# Script de génération de charge pour la démonstration
# Usage: ./scripts/generate-load.sh [duration_seconds]

DURATION=${1:-60}
echo "⚡ Génération de charge pendant ${DURATION} secondes..."

# Vérifier si 'hey' est installé
if ! command -v hey &> /dev/null; then
    echo "❌ 'hey' n'est pas installé."
    echo "Installation:"
    echo "  - macOS: brew install hey"
    echo "  - Linux: go install github.com/rakyll/hey@latest"
    exit 1
fi

# Fonction pour générer de la charge
generate_load() {
    local service=$1
    local endpoint=$2
    local name=$3

    echo "📊 Charge sur $name..."
    hey -z ${DURATION}s -c 5 -q 2 "$endpoint" > /dev/null 2>&1 &
}

# Lancer la charge sur plusieurs endpoints
generate_load "weather" "http://localhost:8081/api/weather/city?city=Paris&country=FR" "Weather Paris"
generate_load "weather" "http://localhost:8081/api/weather/city?city=London&country=GB" "Weather London"
generate_load "air-quality" "http://localhost:8080/api/air-quality/city?city=Paris&country=FR" "Air Quality Paris"
generate_load "health" "http://localhost:8082/api/health/recommendations?city=Paris&country=FR" "Health Paris"

echo "✅ Charge en cours..."
echo "📊 Observez les métriques sur:"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000"
echo ""
echo "⏱️  Durée: ${DURATION}s"

# Attendre la fin
wait

echo ""
echo "✅ Génération de charge terminée!"
