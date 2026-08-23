package com.raglite.embedding;

import com.raglite.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "embedding", name = "provider", havingValue = "gemini")
public class GeminiEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingClient.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiEmbeddingClient(RestClient.Builder restClientBuilder, GeminiProperties properties) {
        this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
        this.properties = properties;
    }

    @Override
    public List<Float> embed(String text) {
        long start = System.nanoTime();
        EmbedResponse response;
        try {
            response = restClient.post()
                    .uri("/{model}:embedContent", properties.embeddingModel())
                    .header("x-goog-api-key", properties.apiKey())
                    .body(new EmbedRequest(new Content(List.of(new Part(text)))))
                    .retrieve()
                    .body(EmbedResponse.class);
        } catch (Exception e) {
            throw new EmbeddingException("Embedding call failed for model " + properties.embeddingModel(), e);
        }

        if (response == null || response.embedding() == null || response.embedding().values().isEmpty()) {
            throw new EmbeddingException("Embedding response had no data for model " + properties.embeddingModel());
        }

        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        log.info("stage=embed model={} latencyMs={}", properties.embeddingModel(), latencyMs);

        return response.embedding().values();
    }

    private record EmbedRequest(Content content) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }

    private record EmbedResponse(Values embedding) {
    }

    private record Values(List<Float> values) {
    }
}
