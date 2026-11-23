package com.edupulse.ingeststatic.ingest;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentIdentityTest {

    @Test
    void docIdIsStableAndIgnoresCase() {
        assertThat(DocumentIdentity.docId("GDPR.pdf"))
                .isEqualTo(DocumentIdentity.docId("gdpr.PDF"))
                .startsWith("doc-")
                .hasSize(20);
    }

    @Test
    void differentFilesGetDifferentDocIds() {
        assertThat(DocumentIdentity.docId("a.pdf")).isNotEqualTo(DocumentIdentity.docId("b.pdf"));
    }

    @Test
    void sourceNameDropsDirectories() {
        assertThat(DocumentIdentity.sourceName("C:\\docs\\policies\\GDPR.pdf")).isEqualTo("GDPR.pdf");
        assertThat(DocumentIdentity.sourceName("/srv/resources/GDPR.pdf")).isEqualTo("GDPR.pdf");
    }

    @Test
    void sourceNameRejectsMissingName() {
        assertThatThrownBy(() -> DocumentIdentity.sourceName("  ")).isInstanceOf(InvalidDocumentException.class);
        assertThatThrownBy(() -> DocumentIdentity.sourceName("folder/")).isInstanceOf(InvalidDocumentException.class);
    }

    @Test
    void checksumChangesWithContent() {
        String first = DocumentIdentity.checksum("v1".getBytes(StandardCharsets.UTF_8));
        String second = DocumentIdentity.checksum("v2".getBytes(StandardCharsets.UTF_8));

        assertThat(first).hasSize(64).isNotEqualTo(second);
        assertThat(DocumentIdentity.checksum("v1".getBytes(StandardCharsets.UTF_8))).isEqualTo(first);
    }

    @Test
    void chunkIdIsDeterministic() {
        assertThat(DocumentIdentity.chunkId("doc-1", 1, 0)).isEqualTo(DocumentIdentity.chunkId("doc-1", 1, 0));
        assertThat(DocumentIdentity.chunkId("doc-1", 1, 0)).isNotEqualTo(DocumentIdentity.chunkId("doc-1", 1, 1));
    }
}
