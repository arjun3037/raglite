package com.raglite.vectorstore;

import com.pgvector.PGvector;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class PgVectorStore implements VectorStore {

    private static final String INSERT_SQL =
            "INSERT INTO chunk (doc_id, content, embedding, token_count) VALUES (?, ?, ?, ?)";

    // Cosine distance ("<=>") is what the ivfflat index (SPEC.md §4) is built for;
    // similarity is reported back as 1 - distance.
    private static final String SEARCH_SQL =
            "SELECT doc_id, content, 1 - (embedding <=> ?) AS similarity "
                    + "FROM chunk ORDER BY embedding <=> ? LIMIT ?";

    private final JdbcTemplate jdbcTemplate;

    public PgVectorStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void store(String docId, List<EmbeddedChunk> chunks) {
        jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int index) throws SQLException {
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
    }

    @Override
    public List<RetrievedChunk> search(List<Float> queryVector, int topK) {
        PGvector vector = toPGvector(queryVector);
        return jdbcTemplate.query(
                SEARCH_SQL,
                (rs, rowNum) -> new RetrievedChunk(
                        rs.getString("doc_id"),
                        rs.getString("content"),
                        rs.getDouble("similarity")),
                vector, vector, topK);
    }

    private PGvector toPGvector(List<Float> embedding) {
        float[] values = new float[embedding.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = embedding.get(i);
        }
        return new PGvector(values);
    }
}
