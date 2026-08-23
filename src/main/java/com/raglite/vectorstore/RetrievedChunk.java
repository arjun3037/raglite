package com.raglite.vectorstore;

public record RetrievedChunk(
        String docId,
        String content,
        double similarity,
        int tokenCount
) {
}
