package com.raglite.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds to {@code raglite.chunking.*}. Named fields here so chunk size and
 * overlap are never inline literals in the chunker (NFR-4).
 */
@Validated
@ConfigurationProperties(prefix = "raglite.chunking")
public record ChunkingProperties(
        @Min(1) int chunkSize,
        @Min(0) int chunkOverlap
) {
}