package com.raglite.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds to {@code openai.*}. A missing api-key fails application startup
 * (NFR-3) rather than surfacing as an NPE on the first request. Only bound
 * when {@code embedding.provider=openai} (see {@link GeminiProperties} for
 * the alternative), so switching providers doesn't require an unused key.
 */
@ConditionalOnProperty(prefix = "embedding", name = "provider", havingValue = "openai", matchIfMissing = true)
@Validated
@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        @NotBlank String apiKey,
        @NotBlank String embeddingModel,
        @NotBlank String chatModel
) {
}