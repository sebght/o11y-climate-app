#!/bin/bash

# Script de test de tous les services
# Usage: ./scripts/test-all-services.sh

set -e

echo "🧪 Test de tous les services..."
echo ""

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Fonction de test
test_endpoint() {
    local name=$1
    local url=$2

    echo -n "Testing $name... "

    if curl -s -f "$url" > /dev/null; then
        echo -e "${GREEN}✓${NC}"
        return 0
    else
        echo -e "${RED}✗${NC}"
        return 1
    fi
}

# Attendre que les services démarrent
echo -e "${YELLOW}⏳ Attente du démarrage des services (30s)...${NC}"
sleep 30

echo ""
echo "=== Health Checks ==="
test_endpoint "Air Quality Service" "http://localhost:8080/api/air-quality/health"
test_endpoint "Weather Service" "http://localhost:8081/health"
test_endpoint "Health Service" "http://localhost:8082/health"
test_endpoint "Frontend" "http://localhost/"
test_endpoint "Prometheus" "http://localhost:9090/-/healthy"
test_endpoint "Grafana" "http://localhost:3000/api/health"

echo ""
echo "=== Metrics Endpoints ==="
test_endpoint "Air Quality Metrics" "http://localhost:8080/actuator/prometheus"
test_endpoint "Weather Metrics" "http://localhost:8081/metrics"
test_endpoint "Health Metrics" "http://localhost:8082/metrics"

echo ""
echo "=== API Endpoints ==="
test_endpoint "Air Quality API (Paris)" "http://localhost:8080/api/air-quality/city?city=Paris&country=FR"
test_endpoint "Weather API (Paris)" "http://localhost:8081/api/weather/city?city=Paris&country=FR"
test_endpoint "Health API (Paris)" "http://localhost:8082/api/health/recommendations?city=Paris&country=FR"

echo ""
echo -e "${GREEN}✅ Tests terminés!${NC}"
echo ""
echo "📊 Accès aux services:"
echo "  - Frontend:   http://localhost"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana:    http://localhost:3000 (admin/admin)"
