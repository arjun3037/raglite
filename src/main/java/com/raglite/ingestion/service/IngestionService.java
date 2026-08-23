package com.raglite.ingestion.service;

import com.raglite.embedding.EmbeddingClient;
import com.raglite.embedding.EmbeddingException;
import com.raglite.ingestion.service.Chunker.TextChunk;
import com.raglite.vectorstore.EmbeddedChunk;
import com.raglite.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final Chunker chunker;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    public IngestionService(Chunker chunker, EmbeddingClient embeddingClient, VectorStore vectorStore) {
        this.chunker = chunker;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    public IngestResult ingest(String docId, String text) {
        List<TextChunk> textChunks = chunker.chunk(text);
        List<EmbeddedChunk> embeddedChunks = new ArrayList<>();
        int failed = 0;

        for (TextChunk textChunk : textChunks) {
            try {
                List<Float> embedding = embeddingClient.embed(textChunk.content());
                embeddedChunks.add(new EmbeddedChunk(textChunk.content(), embedding, textChunk.tokenCount()));
            } catch (EmbeddingException e) {
                failed++;
                log.warn("stage=ingest docId={} event=chunk_embed_failed error={}", docId, e.getMessage(), e);
            }
        }

        if (!embeddedChunks.isEmpty()) {
            vectorStore.store(docId, embeddedChunks);
        }

        log.info("stage=ingest docId={} chunksStored={} chunksFailed={}",
                docId, embeddedChunks.size(), failed);

        return new IngestResult(docId, embeddedChunks.size(), failed);
    }
}
