package com.edupulse.ingeststatic.chunk;

/**
 * A piece of a document that is embedded and stored as one vector.
 *
 * @param text       chunk text
 * @param pageNumber 1-based page the chunk comes from
 * @param chunkIndex 0-based position of the chunk within the whole document
 */
public record Chunk(String text, int pageNumber, int chunkIndex) {
}
