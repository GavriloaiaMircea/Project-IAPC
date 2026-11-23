package com.edupulse.ingeststatic.ingest;

import com.edupulse.ingeststatic.chunk.Chunk;
import com.edupulse.ingeststatic.chunk.StructureAwareChunker;
import com.edupulse.ingeststatic.config.IngestionProperties;
import com.edupulse.ingeststatic.pdf.PageText;
import com.edupulse.ingeststatic.pdf.PdfParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Task 1: parse a PDF, chunk it, embed the chunks and store them with full metadata.
 */
@Service
public class PdfIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PdfIngestionService.class);
    static final int INITIAL_VERSION = 1;

    private final PdfParser parser;
    private final StructureAwareChunker chunker;
    private final VectorStore vectorStore;
    private final ChunkRepository chunkRepository;
    private final IngestionProperties properties;

    public PdfIngestionService(PdfParser parser, StructureAwareChunker chunker, VectorStore vectorStore,
                               ChunkRepository chunkRepository, IngestionProperties properties) {
        this.parser = parser;
        this.chunker = chunker;
        this.vectorStore = vectorStore;
        this.chunkRepository = chunkRepository;
        this.properties = properties;
    }

    public IngestionResult ingest(String source, byte[] content) {
        String sourceName = DocumentIdentity.sourceName(source);
        if (!sourceName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new InvalidDocumentException("Only PDF files are supported: " + sourceName);
        }
        String docId = DocumentIdentity.docId(sourceName);
        String checksum = DocumentIdentity.checksum(content);

        Optional<String> storedChecksum = chunkRepository.findChecksum(docId);
        if (storedChecksum.isPresent()) {
            if (storedChecksum.get().equals(checksum)) {
                int count = chunkRepository.countChunks(docId);
                log.info("Skipped {} ({}): already ingested with the same checksum", sourceName, docId);
                return new IngestionResult(docId, sourceName, IngestionResult.Status.SKIPPED, count,
                        "Already ingested, content unchanged");
            }
            log.warn("Rejected {} ({}): stored with a different checksum", sourceName, docId);
            throw new DocumentConflictException(docId, sourceName);
        }

        List<PageText> pages = parser.parse(content);
        List<Chunk> chunks = chunker.chunk(pages);
        if (chunks.isEmpty()) {
            throw new InvalidDocumentException("No extractable text in " + sourceName + " (scanned PDF?)");
        }

        List<Document> documents = chunks.stream()
                .map(chunk -> toDocument(chunk, docId, sourceName, checksum))
                .toList();
        long start = System.currentTimeMillis();
        vectorStore.add(documents);
        log.info("Ingested {} ({}): {} pages, {} chunks in {} ms",
                sourceName, docId, pages.size(), documents.size(), System.currentTimeMillis() - start);

        return new IngestionResult(docId, sourceName, IngestionResult.Status.INGESTED, documents.size(),
                "Ingested " + pages.size() + " pages");
    }

    /** Ingests every PDF in the configured resources folder; one bad file does not stop the rest. */
    public List<IngestionResult> ingestResourcesFolder() {
        Path folder = Path.of(properties.resourcesDir());
        if (!Files.isDirectory(folder)) {
            throw new IllegalStateException("Resources folder not found: " + folder.toAbsolutePath());
        }
        List<Path> files;
        try (Stream<Path> listing = Files.list(folder)) {
            files = listing.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list " + folder.toAbsolutePath(), e);
        }

        List<IngestionResult> results = new ArrayList<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            try {
                results.add(ingest(name, Files.readAllBytes(file)));
            } catch (IOException | RuntimeException e) {
                log.error("Failed to ingest {}: {}", name, e.getMessage());
                results.add(new IngestionResult(DocumentIdentity.docId(name), name,
                        IngestionResult.Status.FAILED, 0, e.getMessage()));
            }
        }
        log.info("Folder ingestion finished: {} files", results.size());
        return results;
    }

    private static Document toDocument(Chunk chunk, String docId, String sourceName, String checksum) {
        Map<String, Object> metadata = Map.of(
                ChunkMetadata.DOC_ID, docId,
                ChunkMetadata.VERSION, INITIAL_VERSION,
                ChunkMetadata.SOURCE, sourceName,
                ChunkMetadata.PAGE, chunk.pageNumber(),
                ChunkMetadata.CHUNK_INDEX, chunk.chunkIndex(),
                ChunkMetadata.CHECKSUM, checksum);
        return Document.builder()
                .id(DocumentIdentity.chunkId(docId, INITIAL_VERSION, chunk.chunkIndex()).toString())
                .text(chunk.text())
                .metadata(metadata)
                .build();
    }
}
