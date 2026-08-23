package com.raglite.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raglite.config.GeminiProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(prefix = "embedding", name = "provider", havingValue = "gemini")
public class GeminiChatClient implements ChatClient {

    private static final String CHAT_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/{model}:streamGenerateContent";

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiChatClient(RestClient.Builder restClientBuilder,
                            GeminiProperties properties,
                            ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl(CHAT_URL).build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void stream(String prompt, Consumer<String> onToken,
                       Consumer<Throwable> onError, Runnable onComplete) {
        try {
            restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("alt", "sse")
                            .queryParam("key", properties.apiKey())
                            .build(properties.chatModel()))
                    .body(new GenerateContentRequest(
                            List.of(new Content(List.of(new Part(prompt))))))
                    .exchange((request, response) -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data: ")) {
                                    continue;
                                }
                                JsonNode candidates = objectMapper.readTree(
                                        line.substring("data: ".length()))
                                        .path("candidates");
                                for (JsonNode candidate : candidates) {
                                    for (JsonNode part : candidate.path("content").path("parts")) {
                                        JsonNode text = part.path("text");
                                        if (!text.isMissingNode() && !text.isNull()) {
                                            onToken.accept(text.asText());
                                        }
                                    }
                                }
                            }
                        }
                        return null;
                    });
            onComplete.run();
        } catch (Throwable error) {
            onError.accept(error);
        }
    }

    private record GenerateContentRequest(List<Content> contents) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }
}