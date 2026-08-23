package com.raglite.ask.service;

import com.raglite.chat.ChatClient;
import com.raglite.config.ChatProperties;
import com.raglite.config.RetrievalProperties;
import com.raglite.embedding.EmbeddingClient;
import com.raglite.vectorstore.RetrievedChunk;
import com.raglite.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AskService {

    private static final Logger log = LoggerFactory.getLogger(AskService.class);

    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final RetrievalProperties retrievalProperties;
    private final ChatProperties chatProperties;

    public AskService(EmbeddingClient embeddingClient, VectorStore vectorStore,
                      ChatClient chatClient, RetrievalProperties retrievalProperties,
                      ChatProperties chatProperties) {
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.retrievalProperties = retrievalProperties;
        this.chatProperties = chatProperties;
    }

    public void streamAnswer(String question, SseEmitter emitter) {
        CompletableFuture.runAsync(() -> run(question, emitter));
    }

    private void run(String question, SseEmitter emitter) {
        try {
            List<Float> queryVector = embeddingClient.embed(question);
            List<RetrievedChunk> retrieved = vectorStore.search(queryVector, retrievalProperties.topK());
            List<RetrievedChunk> context = selectContext(retrieved);
            log.info("stage=ask questionLength={} chunksRetrieved={} chunksUsed={} contextTokens={}",
                    question.length(), retrieved.size(), context.size(), context.stream()
                            .mapToInt(RetrievedChunk::tokenCount).sum());

            chatClient.stream(buildPrompt(question, context),
                    token -> send(emitter, token),
                    error -> completeWithError(emitter, error),
                    emitter::complete);
        } catch (Throwable error) {
            completeWithError(emitter, error);
        }
    }

    private List<RetrievedChunk> selectContext(List<RetrievedChunk> chunks) {
        int tokenCount = 0;
        int end = 0;
        while (end < chunks.size()
                && tokenCount + chunks.get(end).tokenCount() <= chatProperties.maxContextTokens()) {
            tokenCount += chunks.get(end).tokenCount();
            end++;
        }
        return chunks.subList(0, end);
    }

    private String buildPrompt(String question, List<RetrievedChunk> chunks) {
        StringBuilder prompt = new StringBuilder("Answer the question using only the context below.\n\n");
        for (RetrievedChunk chunk : chunks) {
            prompt.append("[Source ").append(chunk.docId()).append("]\n")
                    .append(chunk.content()).append("\n\n");
        }
        return prompt.append("Question: ").append(question).toString();
    }

    @SuppressWarnings("deprecation")
	private void send(SseEmitter emitter, @NonNull String token) {
        try {
            emitter.send(SseEmitter.event().data(token));
        } catch (IOException error) {
            throw new IllegalStateException("Failed to send answer token", error);
        }
    }

    private void completeWithError(SseEmitter emitter, Throwable error) {
        log.warn("stage=ask event=stream_failed error={}", error.getMessage(), error);
        emitter.completeWithError(error);
    }
}