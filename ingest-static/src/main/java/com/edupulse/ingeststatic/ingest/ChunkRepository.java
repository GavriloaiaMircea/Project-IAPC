package com.edupulse.ingeststatic.ingest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Read-only queries on chunk metadata stored by Spring AI's PgVectorStore.
 * Writes go through the VectorStore API so embeddings are always computed by Spring AI.
 */
@Repository
public class ChunkRepository {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final JdbcTemplate jdbc;
    private final String table;

    public ChunkRepository(JdbcTemplate jdbc,
                           @Value("${spring.ai.vectorstore.pgvector.table-name:vector_store}") String table) {
        if (!SAFE_IDENTIFIER.matcher(table).matches()) {
            throw new IllegalArgumentException("Invalid vector table name: " + table);
        }
        this.jdbc = jdbc;
        this.table = table;
    }

    /** Checksum of the stored chunks of a document, if the document is already ingested. */
    public Optional<String> findChecksum(String docId) {
        List<String> checksums = jdbc.queryForList(
                "SELECT metadata->>'checksum' FROM " + table + " WHERE metadata->>'doc_id' = ? LIMIT 1",
                String.class, docId);
        return checksums.stream().findFirst();
    }

    public int countChunks(String docId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE metadata->>'doc_id' = ?", Integer.class, docId);
        return count == null ? 0 : count;
    }
}
