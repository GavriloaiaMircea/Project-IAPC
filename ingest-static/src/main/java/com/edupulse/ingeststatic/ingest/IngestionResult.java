package com.edupulse.ingeststatic.ingest;

/**
 * Outcome of ingesting one file.
 *
 * @param docId      stable document id (null when the file name itself was invalid)
 * @param source     file name
 * @param status     what happened
 * @param chunkCount chunks stored for the document after this call
 * @param message    human-readable detail
 */
public record IngestionResult(String docId, String source, Status status, int chunkCount, String message) {

    public enum Status {
        /** New document parsed, embedded and stored. */
        INGESTED,
        /** Same file with the same checksum is already stored; nothing was done. */
        SKIPPED,
        /** The file could not be ingested (only used for batch ingestion). */
        FAILED
    }
}
