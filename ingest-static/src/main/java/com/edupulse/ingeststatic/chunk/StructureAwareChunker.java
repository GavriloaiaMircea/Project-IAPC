package com.edupulse.ingeststatic.chunk;

import com.edupulse.ingeststatic.pdf.PageText;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits page text into chunks that follow the document structure.
 * <p>
 * Paragraphs are kept whole whenever they fit. A paragraph that is too long is split
 * at sentence boundaries, and a sentence that is still too long is split between words.
 * Consecutive chunks share an overlap taken from the end of the previous chunk, cut at a
 * word boundary. Chunks never cross page boundaries, so every chunk has an exact page.
 * Sizes are measured in characters; no chunk is longer than {@code chunkSize}.
 */
public class StructureAwareChunker {

    private static final Pattern HYPHENATED_LINE_BREAK = Pattern.compile("(\\p{L})-\\n(\\p{Ll})");
    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\n\\s*\\n");
    private static final Pattern SENTENCE_BREAK = Pattern.compile("(?<=[.!?;:])\\s+");
    private static final Pattern HORIZONTAL_SPACE = Pattern.compile("[ \\t\\x0B\\f]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final String PARAGRAPH_SEPARATOR = "\n\n";
    private static final String SENTENCE_SEPARATOR = " ";

    private final int chunkSize;
    private final int chunkOverlap;

    public StructureAwareChunker(int chunkSize, int chunkOverlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive, was " + chunkSize);
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "chunkOverlap must be >= 0 and smaller than chunkSize, was " + chunkOverlap);
        }
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<Chunk> chunk(List<PageText> pages) {
        List<Chunk> chunks = new ArrayList<>();
        for (PageText page : pages) {
            StringBuilder current = new StringBuilder();
            for (Unit unit : toUnits(page.text())) {
                String separator = unit.startsParagraph() ? PARAGRAPH_SEPARATOR : SENTENCE_SEPARATOR;
                if (!current.isEmpty() && current.length() + separator.length() + unit.text().length() > chunkSize) {
                    String finished = current.toString();
                    chunks.add(new Chunk(finished, page.pageNumber(), chunks.size()));
                    int room = chunkSize - unit.text().length() - separator.length();
                    current = new StringBuilder(overlapTail(finished, room));
                }
                if (!current.isEmpty()) {
                    current.append(separator);
                }
                current.append(unit.text());
            }
            if (!current.isEmpty()) {
                chunks.add(new Chunk(current.toString(), page.pageNumber(), chunks.size()));
            }
        }
        return chunks;
    }

    /** Breaks a page into units no longer than chunkSize: paragraphs, sentences or word groups. */
    private List<Unit> toUnits(String pageText) {
        List<Unit> units = new ArrayList<>();
        for (String paragraph : PARAGRAPH_BREAK.split(normalize(pageText))) {
            String clean = paragraph.strip();
            if (clean.isEmpty()) {
                continue;
            }
            if (clean.length() <= chunkSize) {
                units.add(new Unit(clean, true));
                continue;
            }
            boolean first = true;
            for (String sentence : SENTENCE_BREAK.split(clean)) {
                for (String piece : splitToFit(sentence.strip())) {
                    units.add(new Unit(piece, first));
                    first = false;
                }
            }
        }
        return units;
    }

    /** Normalizes line endings and spacing, and re-joins words hyphenated across lines. */
    private static String normalize(String text) {
        String unixLines = text.replace("\r\n", "\n").replace('\r', '\n');
        String joined = HYPHENATED_LINE_BREAK.matcher(unixLines).replaceAll("$1$2");
        StringBuilder out = new StringBuilder(joined.length());
        for (String line : joined.split("\n", -1)) {
            out.append(HORIZONTAL_SPACE.matcher(line).replaceAll(" ").strip()).append('\n');
        }
        return out.toString();
    }

    /** Splits text longer than chunkSize between words; a single oversized word is cut. */
    private List<String> splitToFit(String text) {
        if (text.length() <= chunkSize) {
            return text.isEmpty() ? List.of() : List.of(text);
        }
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : WHITESPACE.split(text)) {
            if (word.isEmpty()) {
                continue;
            }
            while (word.length() > chunkSize) {
                if (!current.isEmpty()) {
                    pieces.add(current.toString());
                    current.setLength(0);
                }
                pieces.add(word.substring(0, chunkSize));
                word = word.substring(chunkSize);
            }
            if (!current.isEmpty() && current.length() + 1 + word.length() > chunkSize) {
                pieces.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(word);
        }
        if (!current.isEmpty()) {
            pieces.add(current.toString());
        }
        return pieces;
    }

    /** The last characters of a chunk (at most the overlap, at most maxLength), starting at a word. */
    private String overlapTail(String text, int maxLength) {
        int length = Math.min(chunkOverlap, maxLength);
        if (length <= 0) {
            return "";
        }
        int start = text.length() - length;
        if (start <= 0) {
            return text;
        }
        if (!Character.isWhitespace(text.charAt(start - 1))) {
            int nextSpace = indexOfWhitespace(text, start);
            if (nextSpace < 0) {
                return "";
            }
            start = nextSpace + 1;
        }
        return text.substring(start).strip();
    }

    private static int indexOfWhitespace(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private record Unit(String text, boolean startsParagraph) {
    }
}
