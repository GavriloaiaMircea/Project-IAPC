package com.edupulse.ingeststatic.pdf;

import com.edupulse.ingeststatic.ingest.InvalidDocumentException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts text from a PDF, one entry per page.
 * <p>
 * Text is read in content-stream order (not sorted by position), which keeps the
 * columns of multi-column layouts apart instead of interleaving their lines.
 * Paragraph boundaries detected by PDFBox are kept as blank lines so the chunker
 * can split on them.
 */
@Component
public class PdfParser {

    private static final Logger log = LoggerFactory.getLogger(PdfParser.class);
    private static final String PDF_MAGIC = "%PDF-";
    private static final int HEADER_SEARCH_BYTES = 1024;

    public List<PageText> parse(byte[] content) {
        if (!hasPdfHeader(content)) {
            throw new InvalidDocumentException("File is not a PDF");
        }
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(false);
            stripper.setLineSeparator("\n");
            stripper.setParagraphEnd("\n");

            int pageCount = document.getNumberOfPages();
            List<PageText> pages = new ArrayList<>(pageCount);
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(new PageText(page, stripper.getText(document)));
            }
            log.debug("Parsed PDF with {} pages", pageCount);
            return pages;
        } catch (IOException e) {
            throw new InvalidDocumentException("Could not read PDF: " + e.getMessage(), e);
        }
    }

    private static boolean hasPdfHeader(byte[] content) {
        if (content == null || content.length < PDF_MAGIC.length()) {
            return false;
        }
        int limit = Math.min(content.length, HEADER_SEARCH_BYTES);
        String head = new String(content, 0, limit, StandardCharsets.ISO_8859_1);
        return head.contains(PDF_MAGIC);
    }
}
