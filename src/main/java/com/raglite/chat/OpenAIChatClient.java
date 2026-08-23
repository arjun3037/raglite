package com.raglite.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raglite.config.OpenAiProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(prefix = "embedding", name = "provider", havingValue = "openai", matchIfMissing = true)
public class OpenAIChatClient implements ChatClient {

    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAIChatClient(RestClient.Builder restClientBuilder,
                            OpenAiProperties properties,
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
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .body(new ChatRequest(properties.chatModel(), List.of(new Message("user", prompt)), true))
                    .exchange((request, response) -> {
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data: ")) {
                                    continue;
                                }
                                String data = line.substring("data: ".length());
                                if ("[DONE]".equals(data)) {
                                    break;
                                }
                                JsonNode content = objectMapper.readTree(data)
                                        .path("choices").path(0).path("delta").path("content");
                                if (!content.isMissingNode() && !content.isNull()) {
                                    onToken.accept(content.asText());
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

    private record ChatRequest(String model, List<Message> messages, boolean stream) {
    }

    private record Message(String role, String content) {
    }
}