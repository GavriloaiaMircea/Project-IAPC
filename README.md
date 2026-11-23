# EduPulse AI – Static Ingestion

IAPC course project, Epic 1 (RAG), **Story 1: Static ingestion resources**.

## Scope

| Task | Description |
|------|-------------|
| T1 | PDF ingestion: parse, structure-aware chunking with overlap, embed, store with metadata and a stable `doc_id` |
| T2 | Update process: checksum comparison, idempotent re-ingest, atomic replacement of old chunks |
| T3 | Delete process: remove all chunks by `doc_id`, with an orphaned-vector check |
| T4 | Ingestion manifest and re-index support |

## Architecture

- `eureka-server`: service registry
- `api-gateway`: single public entry point (port 8080)
- `ingest-static`: PDF ingestion service
- PostgreSQL + pgvector: vectors and metadata

## Stack

Java 21, Spring Boot 4, Spring Cloud, Spring AI 2, PostgreSQL with pgvector, Docker Compose.

## Running

1. Copy `.env.example` to `.env` and fill in the values.
2. Put the PDFs in `./resources`.
3. Run `docker compose up --build -d`.

_Setup details are added as the modules are implemented._
