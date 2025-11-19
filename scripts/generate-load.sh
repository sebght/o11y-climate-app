#!/bin/bash

# Script de génération de charge pour la démonstration
# Usage: ./scripts/generate-load.sh

echo "⚡ Génération de charge avec k6..."

# Vérifier si 'k6' est installé
if ! command -v k6 &> /dev/null; then
    echo "❌ 'k6' n'est pas installé."
    echo "Installation:"
    echo "  - macOS: brew install k6"
    echo "  - Linux: sudo gpg -k && sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69 && echo 'deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main' | sudo tee /etc/apt/sources.list.d/k6.list && sudo apt-get update && sudo apt-get install k6"
    echo "  - Windows: choco install k6"
    echo "  - Docker: docker pull grafana/k6"
    echo ""
    echo "Plus d'infos: https://k6.io/docs/get-started/installation/"
    exit 1
fi

# Obtenir le chemin du script k6
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K6_SCRIPT="${SCRIPT_DIR}/load-test.js"

if [ ! -f "$K6_SCRIPT" ]; then
    echo "❌ Script k6 introuvable: $K6_SCRIPT"
    exit 1
fi

echo "📊 Lancement du test de charge (durée: 60 secondes)..."
echo "📊 Observez les métriques sur:"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000"
echo ""
echo "💡 Pour personnaliser la durée, éditez scripts/load-test.js"
echo ""

# Lancer k6
k6 run "$K6_SCRIPT"

echo ""
echo "✅ Génération de charge terminée!"
