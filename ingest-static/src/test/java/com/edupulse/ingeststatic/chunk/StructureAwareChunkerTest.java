package com.edupulse.ingeststatic.chunk;

import com.edupulse.ingeststatic.pdf.PageText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StructureAwareChunkerTest {

    @Test
    void keepsSmallParagraphsTogetherInOneChunk() {
        var chunker = new StructureAwareChunker(200, 20);
        var page = new PageText(1, "First paragraph.\n\nSecond paragraph.\n");

        List<Chunk> chunks = chunker.chunk(List.of(page));

        assertThat(chunks).singleElement().satisfies(chunk -> {
            assertThat(chunk.text()).isEqualTo("First paragraph.\n\nSecond paragraph.");
            assertThat(chunk.pageNumber()).isEqualTo(1);
            assertThat(chunk.chunkIndex()).isZero();
        });
    }

    @Test
    void neverExceedsChunkSizeAndAddsOverlap() {
        var chunker = new StructureAwareChunker(120, 30);
        String paragraph = "Controllers must keep records of processing activities. "
                + "Records must be available to the supervisory authority on request. "
                + "Processors must notify the controller without undue delay after a breach. "
                + "Each record must state the purposes of the processing.";

        List<Chunk> chunks = chunker.chunk(List.of(new PageText(1, paragraph)));

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.text().length()).isLessThanOrEqualTo(120));
        for (int i = 1; i < chunks.size(); i++) {
            String previous = chunks.get(i - 1).text();
            String firstWord = chunks.get(i).text().split(" ")[0];
            assertThat(previous).as("chunk %d starts with overlap from chunk %d", i, i - 1).contains(firstWord);
        }
    }

    @Test
    void doesNotCutWordsWhenSplittingLongText() {
        var chunker = new StructureAwareChunker(50, 10);
        String text = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu xi omicron pi rho";

        List<Chunk> chunks = chunker.chunk(List.of(new PageText(1, text)));

        List<String> words = List.of(text.split(" "));
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(words).containsAll(List.of(chunk.text().split("\\s+"))));
    }

    @Test
    void numbersChunksAcrossPagesAndKeepsPageNumbers() {
        var chunker = new StructureAwareChunker(100, 10);
        var pages = List.of(new PageText(1, "Page one text."), new PageText(2, "Page two text."));

        List<Chunk> chunks = chunker.chunk(pages);

        assertThat(chunks).extracting(Chunk::chunkIndex).containsExactly(0, 1);
        assertThat(chunks).extracting(Chunk::pageNumber).containsExactly(1, 2);
    }

    @Test
    void joinsWordsHyphenatedAcrossLines() {
        var chunker = new StructureAwareChunker(100, 10);

        List<Chunk> chunks = chunker.chunk(List.of(new PageText(1, "The regu-\nlation applies.")));

        assertThat(chunks.getFirst().text()).isEqualTo("The regulation applies.");
    }

    @Test
    void returnsNoChunksForBlankPages() {
        var chunker = new StructureAwareChunker(100, 10);

        assertThat(chunker.chunk(List.of(new PageText(1, "  \n\n  ")))).isEmpty();
    }

    @Test
    void rejectsOverlapNotSmallerThanChunkSize() {
        assertThatThrownBy(() -> new StructureAwareChunker(100, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkOverlap");
    }

    @Test
    void rejectsNonPositiveChunkSize() {
        assertThatThrownBy(() -> new StructureAwareChunker(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkSize");
    }
}
