package com.raglite.ingestion.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import com.raglite.config.ChunkingProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Chunker {

    private final Encoding encoding = Encodings.newDefaultEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);
    private final ChunkingProperties properties;

    public Chunker(ChunkingProperties properties) {
        if (properties.chunkOverlap() >= properties.chunkSize()) {
            throw new IllegalStateException(
                    "raglite.chunking.chunk-overlap must be smaller than raglite.chunking.chunk-size");
        }
        this.properties = properties;
    }

    public List<TextChunk> chunk(String text) {
        IntArrayList tokens = encoding.encode(text);
        int totalTokens = tokens.size();
        int step = properties.chunkSize() - properties.chunkOverlap();

        List<TextChunk> chunks = new ArrayList<>();
        for (int start = 0; start < totalTokens; start += step) {
            int end = Math.min(start + properties.chunkSize(), totalTokens);
            IntArrayList slice = new IntArrayList(end - start);
            for (int i = start; i < end; i++) {
                slice.add(tokens.get(i));
            }
            chunks.add(new TextChunk(encoding.decode(slice), slice.size()));
            if (end == totalTokens) {
                break;
            }
        }
        return chunks;
    }

    public record TextChunk(String content, int tokenCount) {
    }
}
