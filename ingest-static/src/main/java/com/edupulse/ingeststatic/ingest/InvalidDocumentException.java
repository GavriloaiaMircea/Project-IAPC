package com.edupulse.ingeststatic.ingest;

/**
 * The uploaded file cannot be ingested: not a PDF, corrupt, encrypted or without text.
 */
public class InvalidDocumentException extends RuntimeException {

    public InvalidDocumentException(String message) {
        super(message);
    }

    public InvalidDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
