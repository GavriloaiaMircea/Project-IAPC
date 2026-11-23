package com.edupulse.ingeststatic.web;

import com.edupulse.ingeststatic.ingest.DocumentConflictException;
import com.edupulse.ingeststatic.ingest.IngestionResult;
import com.edupulse.ingeststatic.ingest.InvalidDocumentException;
import com.edupulse.ingeststatic.ingest.PdfIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IngestionControllerTest {

    private PdfIngestionService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(PdfIngestionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new IngestionController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returns201WhenDocumentIsIngested() throws Exception {
        when(service.ingest(eq("GDPR.pdf"), any())).thenReturn(
                new IngestionResult("doc-1", "GDPR.pdf", IngestionResult.Status.INGESTED, 12, "Ingested 3 pages"));

        mvc.perform(multipart("/ingest").file(pdf("GDPR.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.docId").value("doc-1"))
                .andExpect(jsonPath("$.status").value("INGESTED"))
                .andExpect(jsonPath("$.chunkCount").value(12));
    }

    @Test
    void returns200WhenDocumentIsUnchanged() throws Exception {
        when(service.ingest(eq("GDPR.pdf"), any())).thenReturn(
                new IngestionResult("doc-1", "GDPR.pdf", IngestionResult.Status.SKIPPED, 12, "unchanged"));

        mvc.perform(multipart("/ingest").file(pdf("GDPR.pdf")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"));
    }

    @Test
    void returns400ForInvalidDocument() throws Exception {
        when(service.ingest(any(), any())).thenThrow(new InvalidDocumentException("File is not a PDF"));

        mvc.perform(multipart("/ingest").file(pdf("fake.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("File is not a PDF"));
    }

    @Test
    void returns400ForEmptyUpload() throws Exception {
        mvc.perform(multipart("/ingest").file(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Uploaded file is empty"));
    }

    @Test
    void returns409WhenSameNameHasDifferentContent() throws Exception {
        when(service.ingest(any(), any())).thenThrow(new DocumentConflictException("doc-1", "GDPR.pdf"));

        mvc.perform(multipart("/ingest").file(pdf("GDPR.pdf")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.docId").value("doc-1"));
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.UTF_8));
    }
}
