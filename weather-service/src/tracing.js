const { NodeSDK } = require('@opentelemetry/sdk-node');
const { getNodeAutoInstrumentations } = require('@opentelemetry/auto-instrumentations-node');
const { OTLPTraceExporter } = require('@opentelemetry/exporter-trace-otlp-grpc');
const { Resource } = require('@opentelemetry/resources');
const { SemanticResourceAttributes } = require('@opentelemetry/semantic-conventions');

// Configuration de l'exporteur OTLP
const traceExporter = new OTLPTraceExporter({
  url: process.env.OTEL_EXPORTER_OTLP_ENDPOINT || 'tempo:4317',
});

// Configuration du SDK OpenTelemetry
const sdk = new NodeSDK({
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: 'weather-service',
  }),
  traceExporter,
  instrumentations: [
    getNodeAutoInstrumentations({
      '@opentelemetry/instrumentation-fs': {
        enabled: false, // Désactiver l'instrumentation du système de fichiers
      },
    }),
  ],
});

// Démarrer le SDK
sdk.start();
console.log('[Weather] OpenTelemetry tracing initialized');

// Gérer l'arrêt proprement
process.on('SIGTERM', () => {
  sdk.shutdown()
    .then(() => console.log('[Weather] Tracing terminated'))
    .catch((error) => console.log('[Weather] Error terminating tracing', error))
    .finally(() => process.exit(0));
});

module.exports = sdk;
