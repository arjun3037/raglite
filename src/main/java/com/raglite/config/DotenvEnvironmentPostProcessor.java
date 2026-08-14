package com.raglite.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads a {@code .env} file from the working directory into the Spring
 * {@link ConfigurableEnvironment}, so local runs don't depend on the IDE or
 * shell having exported {@code OPENAI_API_KEY} etc. Silently no-ops if the
 * file doesn't exist (e.g. in CI, where real env vars are used instead).
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) {
            return;
        }

        Map<String, Object> values = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator < 0) {
                    continue;
                }
                values.put(trimmed.substring(0, separator).trim(), trimmed.substring(separator + 1).trim());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read .env file", e);
        }

        environment.getPropertySources().addLast(new MapPropertySource("dotenv", values));
    }
}
