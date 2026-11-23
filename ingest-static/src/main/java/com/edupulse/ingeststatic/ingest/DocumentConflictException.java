package com.edupulse.ingeststatic.ingest;

/**
 * A document with the same name but different content is already stored.
 * Replacing it is the update process (Task 2), not plain ingestion.
 */
public class DocumentConflictException extends RuntimeException {

    private final String docId;

    public DocumentConflictException(String docId, String source) {
        super("Document '" + source + "' (" + docId + ") is already ingested with different content");
        this.docId = docId;
    }

    public String getDocId() {
        return docId;
    }
}
