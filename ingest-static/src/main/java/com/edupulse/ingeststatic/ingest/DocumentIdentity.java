package com.edupulse.ingeststatic.ingest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Stable identifiers for documents and chunks.
 * <ul>
 *   <li>{@code doc_id} depends only on the file name, so it stays the same when the content changes.</li>
 *   <li>{@code checksum} is the SHA-256 of the content and changes with every edit.</li>
 *   <li>Chunk ids are derived from doc_id, version and chunk index, so re-ingesting the same
 *       version overwrites the same rows instead of creating duplicates.</li>
 * </ul>
 */
public final class DocumentIdentity {

    private DocumentIdentity() {
    }

    /** File name without any directory part, e.g. {@code C:\tmp\GDPR.pdf -> GDPR.pdf}. */
    public static String sourceName(String source) {
        if (source == null || source.isBlank()) {
            throw new InvalidDocumentException("File name is missing");
        }
        String normalized = source.replace('\\', '/').strip();
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (name.isEmpty()) {
            throw new InvalidDocumentException("File name is missing");
        }
        return name;
    }

    public static String docId(String sourceName) {
        return "doc-" + sha256Hex(sourceName.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))
                .substring(0, 16);
    }

    public static String checksum(byte[] content) {
        return sha256Hex(content);
    }

    public static UUID chunkId(String docId, int version, int chunkIndex) {
        String key = docId + ":" + version + ":" + chunkIndex;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
