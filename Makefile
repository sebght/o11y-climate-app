.PHONY: help start stop restart logs build clean test health metrics

DOCKER_COMPOSE ?= docker compose

help: ## ⛑️ Affiche cette aide
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

start: ## 🚀 Démarre tous les services
	$(DOCKER_COMPOSE) up -d
	@echo "✅ Services démarrés!"
	@echo "🌐 Frontend: http://localhost"
	@echo "📊 Grafana: http://localhost:3000 (admin/admin)"
	@echo "📈 Prometheus: http://localhost:9090"

stop: ## 💥 Arrête tous les services
	$(DOCKER_COMPOSE) down
	@echo "✅ Services arrêtés"

restart: stop start ## ♻️ Redémarre tous les services

logs: ## 🔎 Affiche les logs de tous les services
	$(DOCKER_COMPOSE) logs -f

build: ## 🏗️ Reconstruit tous les services
	$(DOCKER_COMPOSE) build --no-cache
	@echo "✅ Build terminé"

clean: ## 💥 Supprime tous les conteneurs et volumes
	$(DOCKER_COMPOSE) down -v
	@echo "✅ Nettoyage terminé"

health: ## 🩺 Vérifie la santé de tous les services
	@echo "🔍 Vérification des services..."
	@curl -s http://localhost:8080/api/air-quality/health | jq '.' || echo "❌ Air Quality Service"
	@curl -s http://localhost:8081/health | jq '.' || echo "❌ Weather Service"
	@curl -s http://localhost:8082/health | jq '.' || echo "❌ Health Service"
	@echo "✅ Vérification terminée"

metrics: ## 🛠️ Affiche les endpoints de métriques
	@echo "📊 Endpoints de métriques:"
	@echo "  Air Quality: http://localhost:8080/actuator/prometheus"
	@echo "  Weather:     http://localhost:8081/metrics"
	@echo "  Health:      http://localhost:8082/metrics"
	@echo "  Prometheus:  http://localhost:9090"
	@echo "  Grafana:     http://localhost:3000"

test-paris: ## 🚚 Test rapide avec Paris
	@echo "🗼 Test avec Paris..."
	@curl -s "http://localhost:8082/api/health/recommendations?city=Paris&country=FR" | jq '.'

test-load: ## 🚚 Génère de la charge (nécessite 'k6')
	@./scripts/generate-load.sh 60
