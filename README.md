# RAGlite

RAGlite is a Retrieval-Augmented Generation (RAG) service built from scratch in Java and Spring
Boot. It ingests text into Postgres with `pgvector`, retrieves the most relevant passages for a
given question via cosine similarity, and streams a generated answer back to the client.

The project is designed around clean separation of concerns: model providers and the vector
store sit behind narrow interfaces (`EmbeddingClient`, `ChatClient`, `VectorStore`), so the
underlying implementation can change without touching business logic. It favors explicit,
config-driven behavior over hardcoded values, and structured observability at every pipeline
stage.

## Features

- `POST /ingest` — chunk raw text, generate embeddings, and store them in Postgres/pgvector
- `POST /ask` — embed a question, retrieve the top-K most relevant chunks, and stream a
  generated answer back over SSE
- Fail-soft ingestion: a single failed chunk does not abort the rest of the batch
- Fail-fast configuration: missing required config (e.g. API keys) fails at startup, not at
  request time

## Tech stack

- Java 25, Spring Boot 4
- PostgreSQL with the `pgvector` extension
- Flyway for schema migrations
- OpenAI or Gemini for embeddings (config-selectable), OpenAI for chat completions

## Getting started

### Prerequisites

- Java 25+ and Maven
- A running Postgres instance with the `pgvector` extension (e.g. the `pgvector/pgvector`
  Docker image)
- An API key for your chosen embedding provider (OpenAI or Gemini) and an OpenAI key for chat

### Configuration

Copy `.env.example` to `.env` and fill in your values:

```bash
cp .env.example .env
```

#### Embedding provider

`EMBEDDING_PROVIDER` selects which client handles embeddings — `openai` (default) or `gemini`.
Only the matching provider's API key is required at startup; the other is ignored.

```bash
# .env
EMBEDDING_PROVIDER=gemini
GEMINI_API_KEY=your-gemini-key-here
```

Switching providers on a database that already has stored vectors requires re-ingesting —
different providers produce different embedding dimensions, which breaks similarity search on
existing rows.

### Run

```bash
./mvnw spring-boot:run
```

## Project status

RAGlite is under active development, built in explicit stages — each milestone runs end-to-end
before the next one starts. See [SPEC.md](SPEC.md) for the full design spec, interface
definitions, and the staged roadmap.