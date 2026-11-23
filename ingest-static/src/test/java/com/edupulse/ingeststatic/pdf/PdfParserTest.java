package com.edupulse.ingeststatic.pdf;

import com.edupulse.ingeststatic.ingest.InvalidDocumentException;
import com.edupulse.ingeststatic.support.TestPdfs;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfParserTest {

    private final PdfParser parser = new PdfParser();

    @Test
    void parsesEveryPageWithItsNumber() {
        byte[] pdf = TestPdfs.pdf(
                List.of("Article 1", "Personal data must be processed lawfully."),
                List.of("Article 2", "This regulation applies to automated processing."));

        List<PageText> pages = parser.parse(pdf);

        assertThat(pages).hasSize(2);
        assertThat(pages.get(0).pageNumber()).isEqualTo(1);
        assertThat(pages.get(0).text()).contains("Personal data must be processed lawfully.");
        assertThat(pages.get(1).pageNumber()).isEqualTo(2);
        assertThat(pages.get(1).text()).contains("automated processing");
    }

    @Test
    void rejectsFileThatIsNotPdf() {
        byte[] text = "just some text".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> parser.parse(text))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("not a PDF");
    }

    @Test
    void rejectsCorruptPdf() {
        byte[] corrupt = "%PDF-1.7 this is not really a pdf".getBytes(StandardCharsets.ISO_8859_1);

        assertThatThrownBy(() -> parser.parse(corrupt))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("Could not read PDF");
    }

    @Test
    void rejectsEmptyContent() {
        assertThatThrownBy(() -> parser.parse(new byte[0]))
                .isInstanceOf(InvalidDocumentException.class);
    }
}
