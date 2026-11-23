package com.edupulse.ingeststatic.ingest;

import com.edupulse.ingeststatic.chunk.StructureAwareChunker;
import com.edupulse.ingeststatic.config.IngestionProperties;
import com.edupulse.ingeststatic.pdf.PdfParser;
import com.edupulse.ingeststatic.support.TestPdfs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PdfIngestionServiceTest {

    private static final byte[] SAMPLE_PDF = TestPdfs.pdf(
            List.of("Article 1", "Personal data must be processed lawfully and fairly."),
            List.of("Article 2", "Controllers keep records of processing activities."));

    private VectorStore vectorStore;
    private ChunkRepository chunkRepository;
    private PdfIngestionService service;

    @TempDir
    Path resources;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        chunkRepository = mock(ChunkRepository.class);
        when(chunkRepository.findChecksum(any())).thenReturn(Optional.empty());
        service = new PdfIngestionService(new PdfParser(), new StructureAwareChunker(500, 50), vectorStore,
                chunkRepository, new IngestionProperties(500, 50, resources.toString()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void storesEveryChunkWithFullMetadata() {
        IngestionResult result = service.ingest("policies/GDPR.pdf", SAMPLE_PDF);

        ArgumentCaptor<List<Document>> stored = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(stored.capture());
        List<Document> documents = stored.getValue();

        assertThat(result.status()).isEqualTo(IngestionResult.Status.INGESTED);
        assertThat(result.source()).isEqualTo("GDPR.pdf");
        assertThat(result.chunkCount()).isEqualTo(documents.size()).isEqualTo(2);
        assertThat(documents).allSatisfy(doc -> assertThat(doc.getMetadata()).containsKeys(
                ChunkMetadata.DOC_ID, ChunkMetadata.VERSION, ChunkMetadata.SOURCE,
                ChunkMetadata.PAGE, ChunkMetadata.CHUNK_INDEX, ChunkMetadata.CHECKSUM));
        assertThat(documents).extracting(doc -> doc.getMetadata().get(ChunkMetadata.PAGE)).containsExactly(1, 2);
        assertThat(documents).extracting(doc -> doc.getMetadata().get(ChunkMetadata.DOC_ID))
                .containsOnly(DocumentIdentity.docId("GDPR.pdf"));
        assertThat(documents.getFirst().getId())
                .isEqualTo(DocumentIdentity.chunkId(result.docId(), 1, 0).toString());
    }

    @Test
    void skipsFileThatIsAlreadyIngestedWithSameChecksum() {
        String docId = DocumentIdentity.docId("GDPR.pdf");
        when(chunkRepository.findChecksum(docId)).thenReturn(Optional.of(DocumentIdentity.checksum(SAMPLE_PDF)));
        when(chunkRepository.countChunks(docId)).thenReturn(2);

        IngestionResult result = service.ingest("GDPR.pdf", SAMPLE_PDF);

        assertThat(result.status()).isEqualTo(IngestionResult.Status.SKIPPED);
        assertThat(result.chunkCount()).isEqualTo(2);
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void rejectsSameNameWithDifferentContent() {
        when(chunkRepository.findChecksum(DocumentIdentity.docId("GDPR.pdf"))).thenReturn(Optional.of("old-checksum"));

        assertThatThrownBy(() -> service.ingest("GDPR.pdf", SAMPLE_PDF))
                .isInstanceOf(DocumentConflictException.class);
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void rejectsNonPdfFileName() {
        assertThatThrownBy(() -> service.ingest("notes.txt", "hello".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("Only PDF");
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void rejectsPdfWithoutText() {
        byte[] blank = TestPdfs.pdf(List.of());

        assertThatThrownBy(() -> service.ingest("scan.pdf", blank))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("No extractable text");
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void folderIngestionContinuesAfterAFailedFile() throws IOException {
        Files.write(resources.resolve("a-good.pdf"), SAMPLE_PDF);
        Files.writeString(resources.resolve("b-broken.pdf"), "not a pdf");
        Files.writeString(resources.resolve("readme.txt"), "ignored");

        List<IngestionResult> results = service.ingestResourcesFolder();

        assertThat(results).extracting(IngestionResult::source).containsExactly("a-good.pdf", "b-broken.pdf");
        assertThat(results).extracting(IngestionResult::status)
                .containsExactly(IngestionResult.Status.INGESTED, IngestionResult.Status.FAILED);
    }
}

