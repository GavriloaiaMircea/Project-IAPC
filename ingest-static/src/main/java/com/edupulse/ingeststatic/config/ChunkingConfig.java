package com.edupulse.ingeststatic.config;

import com.edupulse.ingeststatic.chunk.StructureAwareChunker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChunkingConfig {

    @Bean
    public StructureAwareChunker structureAwareChunker(IngestionProperties properties) {
        return new StructureAwareChunker(properties.chunkSize(), properties.chunkOverlap());
    }
}
