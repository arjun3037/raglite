package com.raglite.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.raglite.config.OpenAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OpenAIEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAIEmbeddingClient.class);
    private static final String EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";

    private final RestClient restClient;
    private final OpenAiProperties properties;

    public OpenAIEmbeddingClient(RestClient.Builder restClientBuilder, OpenAiProperties properties) {
        this.restClient = restClientBuilder.baseUrl(EMBEDDINGS_URL).build();
        this.properties = properties;
    }

    @Override
    public List<Float> embed(String text) {
        long start = System.nanoTime();
        EmbeddingResponse response;
        try {
            response = restClient.post()
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .body(new EmbeddingRequest(properties.embeddingModel(), text))
                    .retrieve()
                    .body(EmbeddingResponse.class);
        } catch (Exception e) {
            throw new EmbeddingException("Embedding call failed for model " + properties.embeddingModel(), e);
        }

        if (response == null || response.data().isEmpty()) {
            throw new EmbeddingException("Embedding response had no data for model " + properties.embeddingModel());
        }

        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        log.info("stage=embed model={} latencyMs={} tokens={}",
                properties.embeddingModel(), latencyMs, response.usage().totalTokens());

        return response.data().get(0).embedding();
    }

    private record EmbeddingRequest(String model, String input) {
    }

    private record EmbeddingResponse(List<Datum> data, Usage usage) {
        private record Datum(List<Float> embedding) {
        }

        private record Usage(@JsonProperty("total_tokens") int totalTokens) {
        }
    }
}
