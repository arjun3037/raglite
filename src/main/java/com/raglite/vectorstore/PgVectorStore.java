package com.raglite.vectorstore;

import com.pgvector.PGvector;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class PgVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(PgVectorStore.class);
    private static final int EMBEDDING_DIMENSIONS = 1536;

    private static final String INSERT_SQL =
            "INSERT INTO chunk (doc_id, content, embedding, token_count) VALUES (?, ?, ?, ?)";

    // Cosine distance ("<=>") is what the ivfflat index (SPEC.md §4) is built for;
    // similarity is reported back as 1 - distance.
    private static final String SEARCH_SQL =
            "SELECT doc_id, content, token_count, 1 - (embedding <=> ?::vector) AS similarity "
                    + "FROM chunk ORDER BY embedding <=> ?::vector LIMIT ?";

    private final JdbcTemplate jdbcTemplate;

    public PgVectorStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void store(String docId, List<EmbeddedChunk> chunks) {
        if (chunks.isEmpty()) {
            return;
        }

        for (EmbeddedChunk chunk : chunks) {
            if (chunk.embedding().size() != EMBEDDING_DIMENSIONS) {
                throw new IllegalArgumentException("Expected embedding dimension "
                        + EMBEDDING_DIMENSIONS + " but received " + chunk.embedding().size());
            }
        }

         int[] insertedRows = jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@SuppressWarnings("null") PreparedStatement ps, int index) throws SQLException {
                EmbeddedChunk chunk = chunks.get(index);
                ps.setString(1, docId);
                ps.setString(2, chunk.content());
                ps.setObject(3, toPGvector(chunk.embedding()));
                ps.setInt(4, chunk.tokenCount());
            }

            @Override
            public int getBatchSize() {
                return chunks.size();
            }
        });
        log.info("stage=vector_store_after_insert sql={} docId={} chunksInserted={} insertedRows={}",
                INSERT_SQL, docId, chunks.size(), insertedRows);
    }

    @Override
    public List<RetrievedChunk> search(List<Float> queryVector, int topK) {
        PGvector vector = toPGvector(queryVector);
        List<RetrievedChunk> results = jdbcTemplate.query(
                SEARCH_SQL,
                (rs, rowNum) -> new RetrievedChunk(
                        rs.getString("doc_id"),
                        rs.getString("content"),
                        rs.getDouble("similarity"),
                        rs.getInt("token_count")),
                vector, vector, topK);

        log.info("stage=vector_search queryDimensions={} topK={} results={}",
                queryVector.size(), topK, results.size());

        return results;
    }

    private PGvector toPGvector(List<Float> embedding) {
        float[] values = new float[embedding.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = embedding.get(i);
        }
        return new PGvector(values);
    }
}
