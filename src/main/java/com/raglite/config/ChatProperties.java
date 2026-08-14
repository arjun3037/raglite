package com.raglite.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds to {@code raglite.chat.*}. Max context tokens governs prompt
 * assembly and is never a literal in that code (NFR-4).
 */
@Validated
@ConfigurationProperties(prefix = "raglite.chat")
public record ChatProperties(
        @Min(1) int maxContextTokens
) {
}