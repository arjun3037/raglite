package com.raglite.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds to {@code gemini.*}. Only required when {@code embedding.provider=gemini}
 * (see {@link OpenAiProperties} for the OpenAI counterpart).
 */
@ConditionalOnProperty(prefix = "embedding", name = "provider", havingValue = "gemini")
@Validated
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        @NotBlank String apiKey,
        @NotBlank String embeddingModel
) {
}
