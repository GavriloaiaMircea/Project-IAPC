package com.edupulse.ingeststatic.web;

import com.edupulse.ingeststatic.ingest.DocumentConflictException;
import com.edupulse.ingeststatic.ingest.InvalidDocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Maps ingestion errors to RFC 9457 problem responses.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidDocumentException.class)
    public ProblemDetail invalidDocument(InvalidDocumentException e) {
        log.warn("Invalid document: {}", e.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Invalid document", e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail missingFile(MissingServletRequestPartException e) {
        return problem(HttpStatus.BAD_REQUEST, "Missing file", "Send the PDF as multipart field 'file'");
    }

    @ExceptionHandler(DocumentConflictException.class)
    public ProblemDetail conflict(DocumentConflictException e) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Document already ingested", e.getMessage());
        problem.setProperty("docId", e.getDocId());
        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail tooLarge(MaxUploadSizeExceededException e) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "File too large", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
