package com.edupulse.ingeststatic.ingest;

/**
 * Metadata keys stored with every chunk in the vector store.
 */
public final class ChunkMetadata {

    public static final String DOC_ID = "doc_id";
    public static final String VERSION = "version";
    public static final String SOURCE = "source";
    public static final String PAGE = "page";
    public static final String CHUNK_INDEX = "chunk_index";
    public static final String CHECKSUM = "checksum";

    private ChunkMetadata() {
    }
}
