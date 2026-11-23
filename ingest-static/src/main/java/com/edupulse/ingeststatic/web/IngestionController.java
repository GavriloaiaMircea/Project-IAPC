package com.edupulse.ingeststatic.web;

import com.edupulse.ingeststatic.ingest.IngestionResult;
import com.edupulse.ingeststatic.ingest.InvalidDocumentException;
import com.edupulse.ingeststatic.ingest.PdfIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/ingest")
public class IngestionController {

    private final PdfIngestionService ingestionService;

    public IngestionController(PdfIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /** Upload one PDF. 201 when it was ingested, 200 when it was already there unchanged. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IngestionResult> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidDocumentException("Uploaded file is empty");
        }
        IngestionResult result = ingestionService.ingest(file.getOriginalFilename(), file.getBytes());
        HttpStatus status = result.status() == IngestionResult.Status.INGESTED ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result);
    }

    /** Ingest every PDF in the configured resources folder. */
    @PostMapping("/resources")
    public List<IngestionResult> ingestResources() {
        return ingestionService.ingestResourcesFolder();
    }
}
