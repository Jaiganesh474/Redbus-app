package com.redbus.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kb_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KbChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private KbDocument document;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "chunk_index", nullable = false)
    @Builder.Default
    private Integer chunkIndex = 0;
}
