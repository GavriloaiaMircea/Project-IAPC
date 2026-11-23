# Context for Claude

IAPC course project (master's), solo team. Epic 1 (RAG), **Story 1: Static ingestion resources**.
Epic 1 deadline: **December 7, 2026**. Grading: 1 point per task; each task is presented to the
lab instructor (product owner) in short weekly progress meetings.

## Tasks (official list: "tasks for first epic" PDF)

- **T1 PDF ingestion** (done): parse, structure-aware chunking with overlap, embed, store with
  metadata (`doc_id`, `version`, `source`, `page`, `chunk_index`, `checksum`) and stable `doc_id`.
- **T2 Update process**: changed file detected by checksum, unchanged skipped; old chunks replaced
  atomically (queries never see mixed or empty results); a failure mid-update keeps the previous
  version; test that the chunk count after update matches.
- **T3 Delete process**: delete by `doc_id` removes all chunks; deleted content no longer returned;
  orphan check reports zero leftover vectors; deleting a missing document is safe with a clear message.
- **T4 Manifest and re-index**: manifest records `doc_id`, checksum, version, timestamp, embedding
  model, chunk count; reconciliation shows zero drift between manifest and vector DB; full re-index
  with a different embedding model works on the sample set.

Baseline for every task: tests for a success and a failure path, no secrets in the repo,
configuration documented, errors and key events logged, README updated, merged to `main` via PR.

## Decisions already made

- Embeddings: Ollama in Docker (`nomic-embed-text`, 768 dims), no API key.
- pgvector container on host port **5433** (5432 may be taken by a local PostgreSQL).
- Spring Boot 4.0.10, Spring AI 2.0.1, Spring Cloud 2025.1.5, Java 21, PDFBox 3.0.7.
- `doc_id` comes from the file name; same name + different content returns 409 until T2 adds update.
- Chunks never cross page boundaries (exact page metadata).
- `eureka-server` and `api-gateway` are planned after the tasks; they are not part of any task.

## Working rules

- **Never run `git commit` or `git push`.** The user commits. Propose small commits
  (several per task) with short messages and **no Co-Authored-By or other attribution lines**.
- Start every message with the current task tag: **[Setup]**, **[T1]** … **[T4]**, and say
  explicitly when a task starts and when it is finished.
- The user writes in Romanian; answer in Romanian.
- Test everything that can be tested: unit tests (`./mvnw -pl ingest-static test`) and an
  end-to-end run with Docker (see README).
