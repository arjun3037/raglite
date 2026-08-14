package com.raglite.vectorstore;

import java.util.List;

public interface VectorStore {

    void store(String docId, List<EmbeddedChunk> chunks);

    List<RetrievedChunk> search(List<Float> queryVector, int topK);
}
