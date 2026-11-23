package com.edupulse.ingeststatic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ingestion settings, bound from {@code app.ingest.*} in application.yaml.
 *
 * @param chunkSize     maximum chunk length in characters
 * @param chunkOverlap  characters repeated from the end of the previous chunk
 * @param resourcesDir  folder scanned by {@code POST /ingest/resources}
 */
@ConfigurationProperties("app.ingest")
public record IngestionProperties(int chunkSize, int chunkOverlap, String resourcesDir) {
}
