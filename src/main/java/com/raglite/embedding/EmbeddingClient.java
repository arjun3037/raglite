package com.raglite.embedding;

import java.util.List;

public interface EmbeddingClient {

    List<Float> embed(String text);
}
