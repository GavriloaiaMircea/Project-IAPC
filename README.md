# EduPulse AI – Static Ingestion

IAPC course project, Epic 1 (RAG), **Story 1: Static ingestion resources**.

## Scope

| Task | Description | Status |
|------|-------------|--------|
| T1 | PDF ingestion: parse, structure-aware chunking with overlap, embed, store with metadata and a stable `doc_id` | done |
| T2 | Update process: checksum comparison, idempotent re-ingest, atomic replacement of old chunks | todo |
| T3 | Delete process: remove all chunks by `doc_id`, with an orphaned-vector check | todo |
| T4 | Ingestion manifest and re-index support | todo |

## Architecture

- `ingest-static`: PDF ingestion service (port 8081)
- PostgreSQL + pgvector: vectors and metadata (table `vector_store`, HNSW index, cosine distance)
- Ollama: local embedding model, called through Spring AI
- Planned: `eureka-server` (service registry) and `api-gateway` (single public entry point, port 8080)

## Stack

Java 21, Spring Boot 4.0, Spring AI 2.0, PostgreSQL 17 with pgvector, Ollama, Docker Compose.

## Running locally

Requirements: JDK 21 and Docker Desktop. Maven is not needed, the wrapper (`mvnw`) downloads it.

1. Copy `.env.example` to `.env` and set `POSTGRES_PASSWORD`.
2. Start the database and the embedding model:
   ```
   docker compose up -d
   ```
3. Start the service (the first start downloads the embedding model, about 270 MB):
   ```
   ./mvnw -pl ingest-static spring-boot:run
   ```
4. Check it is up: `curl localhost:8081/actuator/health`

## Configuration

All settings are environment variables, read from `.env` (never committed).

| Variable | Default | Meaning |
|----------|---------|---------|
| `POSTGRES_PASSWORD` | – (required) | Database password |
| `POSTGRES_DB`, `POSTGRES_USER` | `edupulse` | Database name and user |
| `DB_PORT` | `5433` | Host port of the pgvector container (5433 avoids a locally installed PostgreSQL) |
| `EMBEDDING_MODEL` | `nomic-embed-text` | Ollama embedding model, pulled automatically if missing |
| `EMBEDDING_DIMENSIONS` | `768` | Must match the model; changing the model means re-indexing |
| `APP_CHUNK_SIZE` | `1000` | Maximum chunk length, in characters |
| `APP_CHUNK_OVERLAP` | `150` | Characters repeated from the end of the previous chunk |
| `APP_RESOURCES_DIR` | `../resources` | Folder ingested by `POST /ingest/resources` |

## Task 1: PDF ingestion

### How it works

1. **Parse** (`PdfParser`): PDFBox extracts the text page by page, in content-stream order, so
   the columns of a two-column page are read one after the other instead of line by line.
2. **Chunk** (`StructureAwareChunker`): paragraphs are kept whole when they fit; long paragraphs
   are split at sentence boundaries, then between words. Consecutive chunks overlap by up to
   `APP_CHUNK_OVERLAP` characters, cut at a word boundary. Chunks never cross pages, so every
   chunk has an exact page number. Words hyphenated across lines are joined again.
3. **Embed and store** (`PdfIngestionService`): chunks are embedded by Spring AI and stored in
   pgvector with this metadata:

   | Key | Example | Meaning |
   |-----|---------|---------|
   | `doc_id` | `doc-de603175208b06a9` | Stable id derived from the file name (same name, same id) |
   | `version` | `1` | Document version |
   | `source` | `data-protection-policy.pdf` | File name |
   | `page` | `2` | 1-based page number |
   | `chunk_index` | `0` | Position of the chunk in the document |
   | `checksum` | `db3b05ab…` | SHA-256 of the file content |

4. **No duplicates**: if a file with the same name and checksum is already stored, it is skipped.
   Chunk ids are derived from `doc_id`, version and chunk index, so even a forced re-run would
   overwrite the same rows. The same name with different content returns `409 Conflict`
   (replacing a document is the update process, Task 2).

### API

| Method | Path | Result |
|--------|------|--------|
| `POST` | `/ingest` (multipart field `file`) | `201` ingested, `200` already ingested and unchanged, `400` not a valid PDF or no text, `409` same name with different content |
| `POST` | `/ingest/resources` | Ingests every PDF in the resources folder; a failed file is reported as `FAILED` and does not stop the others |

```
curl -F "file=@resources/data-protection-policy.pdf" localhost:8081/ingest
curl -X POST localhost:8081/ingest/resources
```

### Sample PDFs

`resources/` contains three generated samples: a two-page policy, a table
(`retention-schedule-table.pdf`) and a two-column report (`audit-report-two-columns.pdf`).
To regenerate them:

```
./mvnw -q -pl ingest-static test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt -Dmdep.includeScope=test
java -cp "ingest-static/target/test-classes;$(cat ingest-static/target/cp.txt)" com.edupulse.ingeststatic.support.SamplePdfGenerator resources
```

(On Linux or macOS use `:` instead of `;` in the classpath.)

### Manual chunk check

Prints 20 random chunks with their metadata, to check that no text is broken or garbled:

```
docker exec edupulse-pgvector psql -U edupulse -d edupulse -c "SELECT metadata->>'source' AS source, metadata->>'page' AS page, content FROM vector_store ORDER BY random() LIMIT 20"
```

With the default chunk size the three samples give 5 chunks. To get more (22 with these samples),
empty the table, restart the service with `APP_CHUNK_SIZE=300` and `APP_CHUNK_OVERLAP=60`, and
ingest again (unchanged files are otherwise skipped):

```
docker exec edupulse-pgvector psql -U edupulse -d edupulse -c "TRUNCATE vector_store"
```

## Tests

```
./mvnw -pl ingest-static test
```

Unit tests cover the parser, the chunker, the identifiers, the ingestion service and the REST
controller, each with success and failure cases. They need neither Docker nor Ollama.
