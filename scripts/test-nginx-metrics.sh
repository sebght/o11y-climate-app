#!/bin/bash

echo "========================================="
echo "Test des métriques Nginx"
echo "========================================="

# Couleurs pour l'affichage
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

# Fonction de test
test_endpoint() {
    local url=$1
    local description=$2

    echo -e "\nTest: $description"
    echo "URL: $url"

    response=$(curl -s -o /dev/null -w "%{http_code}" "$url")

    if [ "$response" = "200" ]; then
        echo -e "${GREEN}✓ OK${NC} (HTTP $response)"
        return 0
    else
        echo -e "${RED}✗ ERREUR${NC} (HTTP $response)"
        return 1
    fi
}

# Attendre que les services démarrent
echo "Attente du démarrage des services..."
sleep 5

# Tests
errors=0

# Test 1: Vérifier que le frontend est accessible
test_endpoint "http://localhost:80" "Frontend accessible" || ((errors++))

# Test 2: Vérifier le endpoint nginx_status
echo -e "\nTest: Nginx stub_status"
echo "URL: http://localhost:80/nginx_status"
response=$(curl -s http://localhost:80/nginx_status)
if echo "$response" | grep -q "Active connections"; then
    echo -e "${GREEN}✓ OK${NC}"
    echo "Extrait:"
    echo "$response" | head -3
else
    echo -e "${RED}✗ ERREUR${NC}"
    ((errors++))
fi

# Test 3: Vérifier que nginx-exporter expose les métriques
echo -e "\nTest: Nginx Exporter métriques"
echo "URL: http://localhost:9113/metrics"
response=$(curl -s http://localhost:9113/metrics)
if echo "$response" | grep -q "nginx_"; then
    echo -e "${GREEN}✓ OK${NC}"
    echo "Exemples de métriques:"
    echo "$response" | grep "nginx_" | grep -v "^#" | head -5
else
    echo -e "${RED}✗ ERREUR${NC}"
    ((errors++))
fi

# Test 4: Vérifier que Prometheus scrape nginx
echo -e "\nTest: Prometheus configuration pour nginx"
echo "URL: http://localhost:9090/api/v1/targets"
response=$(curl -s http://localhost:9090/api/v1/targets)
if echo "$response" | grep -q "nginx"; then
    echo -e "${GREEN}✓ OK${NC}"
    echo "Target nginx trouvé dans Prometheus"
else
    echo -e "${RED}✗ ERREUR${NC}"
    ((errors++))
fi

# Résumé
echo -e "\n========================================="
if [ $errors -eq 0 ]; then
    echo -e "${GREEN}Tous les tests sont passés avec succès!${NC}"
    echo ""
    echo "Accès aux services:"
    echo "  - Frontend: http://localhost:80"
    echo "  - Nginx Status: http://localhost:80/nginx_status"
    echo "  - Nginx Exporter: http://localhost:9113/metrics"
    echo "  - Prometheus: http://localhost:9090"
    echo "  - Grafana: http://localhost:3000"
else
    echo -e "${RED}$errors test(s) échoué(s)${NC}"
    exit 1
fi
echo "========================================="
