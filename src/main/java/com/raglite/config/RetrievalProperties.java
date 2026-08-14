package com.raglite.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds to {@code raglite.retrieval.*}. top-k lives here, not as a literal
 * in the retrieval service (NFR-4).
 */
@Validated
@ConfigurationProperties(prefix = "raglite.retrieval")
public record RetrievalProperties(
        @Min(1) int topK
) {
}