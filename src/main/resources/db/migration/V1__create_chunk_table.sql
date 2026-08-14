CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE chunk (
    id           BIGSERIAL PRIMARY KEY,
    doc_id       TEXT NOT NULL,
    content      TEXT NOT NULL,
    embedding    VECTOR(1536) NOT NULL,
    token_count  INTEGER NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ON chunk USING ivfflat (embedding vector_cosine_ops);
