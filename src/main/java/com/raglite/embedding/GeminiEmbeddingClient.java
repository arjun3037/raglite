package com.raglite.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.raglite.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;

@Component
public class GeminiEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingClient.class);
    private static final String EMBEDDINGS_URL = "https://generativelanguage.googleapis.com/v1beta/models/{model}:embedContent?key={apiKey}";

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiEmbeddingClient(RestClient.Builder restClientBuilder, GeminiProperties properties) {
        this.restClient = restClientBuilder.baseUrl(EMBEDDINGS_URL).build();
        this.properties = properties;
    }

    @Override
    public List<Float> embed(String text) {
        long start = System.nanoTime();
        EmbeddingResponse response;
        try {
            response = restClient.post()
                .uri(uriBuilder -> uriBuilder.build(
                    properties.embeddingModel(),
                    properties.apiKey()))
                .body(new EmbeddingRequest(
                    "models/" + properties.embeddingModel(),
                    new Content(new Part(text)),
                    properties.embeddingDimensions()))
                    .retrieve()
                    .body(EmbeddingResponse.class);
        } catch (Exception e) {
            throw new EmbeddingException("Embedding call failed for model " + properties.embeddingModel(), e);
        }

        if (response == null || response.embedding() == null || response.embedding().values().isEmpty()) {
            throw new EmbeddingException("Embedding response had no data for model " + properties.embeddingModel());
        }

        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        log.info("stage=embed model={} latencyMs={} dimensions={}",
                properties.embeddingModel(), latencyMs, response.embedding().values().size());

        return response.embedding().values();
    }

    private record EmbeddingRequest(
            String model,
            Content content,
            @JsonProperty("output_dimensionality") int outputDimensionality) {
    }

    private record Content(List<Part> parts) {
        private Content(Part part) {
            this(List.of(part));
        }
    }

    private record Part(String text) {
    }

    private record EmbeddingResponse(Embedding embedding) {
    }

    private record Embedding(List<Float> values) {
    }
}
