package com.raglite.vectorstore;

import java.util.List;

public record EmbeddedChunk(
        String content,
        List<Float> embedding,
        int tokenCount
) {
}
