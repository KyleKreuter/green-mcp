package de.kyle.greenmcp.entity;

import de.kyle.greenmcp.converter.VectorConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "beschluesse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Beschluss {

    @Id
    private UUID id;

    @Column(name = "pdf_url")
    private String pdfUrl;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String title;

    private String topic;

    private String filename;

    @Column(name = "word_count")
    private Integer wordCount;

    @Convert(converter = VectorConverter.class)
    @Column(columnDefinition = "vector(1024)")
    private float[] embedding;
}
