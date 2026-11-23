package com.edupulse.ingeststatic.support;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the sample PDFs in ./resources used for the end-to-end ingestion demo:
 * a multi-page policy, a table and a two-column report.
 * <p>
 * Run from the project root (see README, "Sample PDFs"):
 * <pre>
 * ./mvnw -q -pl ingest-static test-compile dependency:build-classpath \
 *     -Dmdep.outputFile=target/cp.txt -Dmdep.includeScope=test
 * java -cp "ingest-static/target/test-classes;$(cat ingest-static/target/cp.txt)" \
 *     com.edupulse.ingeststatic.support.SamplePdfGenerator resources
 * </pre>
 */
public final class SamplePdfGenerator {

    private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float MARGIN = 56;
    private static final float FONT_SIZE = 10.5f;
    private static final float LEADING = 14;

    private SamplePdfGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "resources");
        Files.createDirectories(out);
        policy(out.resolve("data-protection-policy.pdf"));
        retentionTable(out.resolve("retention-schedule-table.pdf"));
        twoColumnReport(out.resolve("audit-report-two-columns.pdf"));
        System.out.println("Sample PDFs written to " + out.toAbsolutePath());
    }

    // ---------------------------------------------------------------- multi-page policy

    private static void policy(Path file) throws IOException {
        List<String[]> sections = List.of(
                new String[]{"1. Purpose",
                        "This policy defines how EduPulse processes personal data of students, staff and partners. "
                                + "It applies to every department, contractor and information system that stores or "
                                + "transmits personal data on behalf of the organisation.",
                        "The policy implements the principles of lawfulness, fairness and transparency, purpose "
                                + "limitation, data minimisation, accuracy, storage limitation, integrity and "
                                + "confidentiality, and accountability."},
                new String[]{"2. Roles and responsibilities",
                        "The Data Protection Officer monitors compliance, advises on data protection impact "
                                + "assessments and acts as the contact point for the supervisory authority.",
                        "Department heads are accountable for the processing activities in their area. They keep "
                                + "the record of processing activities up to date and review it at least once a year.",
                        "Every employee completes data protection training within thirty days of joining and "
                                + "repeats it every two years."},
                new String[]{"3. Lawful basis and consent",
                        "Personal data is processed only when a lawful basis exists: consent, contract, legal "
                                + "obligation, vital interests, public task or legitimate interests.",
                        "Where consent is the lawful basis, it must be freely given, specific, informed and "
                                + "unambiguous. Withdrawing consent must be as easy as giving it, and the withdrawal "
                                + "is recorded within two working days."},
                new String[]{"4. Data subject rights",
                        "Requests for access, rectification, erasure, restriction, portability or objection are "
                                + "answered within one month. The deadline may be extended by two further months for "
                                + "complex requests, in which case the data subject is informed of the extension.",
                        "Identity is verified before any personal data is disclosed. Requests are logged in the "
                                + "rights register together with the date of receipt and the date of the answer."},
                new String[]{"5. Personal data breaches",
                        "Any suspected breach is reported to the Data Protection Officer immediately. Breaches "
                                + "that are likely to result in a risk to individuals are notified to the supervisory "
                                + "authority within 72 hours of becoming aware of them.",
                        "When the breach is likely to result in a high risk, the affected data subjects are also "
                                + "informed without undue delay, in clear and plain language."},
                new String[]{"6. International transfers",
                        "Personal data is transferred outside the European Economic Area only to countries with an "
                                + "adequacy decision or under appropriate safeguards such as standard contractual "
                                + "clauses, after a transfer impact assessment."},
                new String[]{"7. Review",
                        "This policy is reviewed every year by the Data Protection Officer and approved by the "
                                + "management board. The current version is 3.2, approved in September 2026."});

        try (PDDocument document = new PDDocument()) {
            TextWriter writer = new TextWriter(document);
            writer.heading("EduPulse Data Protection Policy", 16);
            for (String[] section : sections) {
                writer.heading(section[0], 12);
                for (int i = 1; i < section.length; i++) {
                    writer.paragraph(section[i], PDRectangle.LETTER.getWidth() - 2 * MARGIN);
                }
            }
            writer.close();
            document.save(file.toFile());
        }
    }

    // ---------------------------------------------------------------- table

    private static void retentionTable(Path file) throws IOException {
        String[] header = {"Record type", "Retention", "Legal basis", "Owner"};
        String[][] rows = {
                {"Student enrolment file", "10 years", "Legal obligation", "Registrar"},
                {"Exam results", "50 years", "Public task", "Faculty office"},
                {"Job applications", "6 months", "Consent", "HR"},
                {"Payroll records", "5 years", "Legal obligation", "Finance"},
                {"CCTV footage", "30 days", "Legitimate interests", "Security"},
                {"Access logs", "1 year", "Legitimate interests", "IT"},
                {"Research data", "10 years", "Public task", "Research office"},
                {"Marketing contacts", "2 years", "Consent", "Communications"},
        };
        float[] widths = {170, 90, 140, 100};
        float rowHeight = 22;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - MARGIN;
                text(stream, BOLD, 16, MARGIN, y, "Records Retention Schedule");
                y -= 22;
                text(stream, REGULAR, FONT_SIZE, MARGIN, y,
                        "Retention periods for personal data, approved by the Data Protection Officer.");
                y -= 30;

                float tableTop = y;
                List<String[]> all = new ArrayList<>();
                all.add(header);
                all.addAll(List.of(rows));
                for (int r = 0; r < all.size(); r++) {
                    float x = MARGIN;
                    for (int c = 0; c < widths.length; c++) {
                        text(stream, r == 0 ? BOLD : REGULAR, FONT_SIZE, x + 5, y - 15, all.get(r)[c]);
                        x += widths[c];
                    }
                    y -= rowHeight;
                }
                float tableWidth = 0;
                for (float w : widths) {
                    tableWidth += w;
                }
                for (int r = 0; r <= all.size(); r++) {
                    line(stream, MARGIN, tableTop - r * rowHeight, MARGIN + tableWidth, tableTop - r * rowHeight);
                }
                float x = MARGIN;
                for (int c = 0; c <= widths.length; c++) {
                    line(stream, x, tableTop, x, y);
                    if (c < widths.length) {
                        x += widths[c];
                    }
                }
                text(stream, REGULAR, FONT_SIZE, MARGIN, y - 30,
                        "Records are deleted or anonymised at the end of the retention period.");
            }
            document.save(file.toFile());
        }
    }

    // ---------------------------------------------------------------- two columns

    private static void twoColumnReport(Path file) throws IOException {
        String left = "Internal audit 2026: summary. The audit reviewed how personal data is handled in the "
                + "admissions, finance and IT departments between January and June 2026. "
                + "Admissions keeps paper enrolment forms in locked cabinets, but scanned copies are also stored "
                + "on a shared drive without access restrictions. "
                + "Finance applies the payroll retention period correctly and deletes records older than five "
                + "years every quarter.";
        String right = "Findings and actions. Finding A1: the shared admissions drive must be restricted to "
                + "registrar staff by the end of September. "
                + "Finding A2: twelve employees in IT have not completed the mandatory data protection training. "
                + "Finding A3: the breach register is complete and every breach was assessed within 72 hours. "
                + "The overall rating is satisfactory with three corrective actions.";
        float pageWidth = PDRectangle.LETTER.getWidth();
        float gap = 24;
        float columnWidth = (pageWidth - 2 * MARGIN - gap) / 2;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float top = page.getMediaBox().getHeight() - MARGIN;
                text(stream, BOLD, 16, MARGIN, top, "Internal Audit Report - Data Protection");
                float columnTop = top - 34;
                // Left column is written completely before the right one, like real two-column layouts.
                column(stream, left, MARGIN, columnTop, columnWidth);
                column(stream, right, MARGIN + columnWidth + gap, columnTop, columnWidth);
            }
            document.save(file.toFile());
        }
    }

    private static void column(PDPageContentStream stream, String text, float x, float y, float width)
            throws IOException {
        stream.beginText();
        stream.setFont(REGULAR, FONT_SIZE);
        stream.setLeading(LEADING);
        stream.newLineAtOffset(x, y);
        for (String line : wrap(text, REGULAR, FONT_SIZE, width)) {
            stream.showText(line);
            stream.newLine();
        }
        stream.endText();
    }

    // ---------------------------------------------------------------- helpers

    private static void text(PDPageContentStream stream, PDType1Font font, float size, float x, float y, String s)
            throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(x, y);
        stream.showText(s);
        stream.endText();
    }

    private static void line(PDPageContentStream stream, float x1, float y1, float x2, float y2) throws IOException {
        stream.moveTo(x1, y1);
        stream.lineTo(x2, y2);
        stream.stroke();
    }

    static List<String> wrap(String text, PDType1Font font, float size, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000 * size > width && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    /** Writes headings and wrapped paragraphs top to bottom, starting new pages as needed. */
    private static final class TextWriter {
        private final PDDocument document;
        private PDPageContentStream stream;
        private float y;

        TextWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        void heading(String text, float size) throws IOException {
            ensureSpace(size + LEADING * 3);
            y -= LEADING;
            text(stream, BOLD, size, MARGIN, y, text);
            y -= LEADING + 4;
        }

        void paragraph(String text, float width) throws IOException {
            for (String line : wrap(text, REGULAR, FONT_SIZE, width)) {
                ensureSpace(LEADING);
                text(stream, REGULAR, FONT_SIZE, MARGIN, y, line);
                y -= LEADING;
            }
            y -= LEADING; // blank line between paragraphs
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < MARGIN + 140) {
                newPage();
            }
        }

        private void newPage() throws IOException {
            close();
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
