package de.kyle.greenmcp.repository;

import de.kyle.greenmcp.entity.Beschluss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BeschlussRepository extends JpaRepository<Beschluss, UUID> {

    @Query(value = """
        SELECT * FROM beschluesse
        ORDER BY embedding <=> cast(:embedding as vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Beschluss> findByEmbeddingSimilarity(
        @Param("embedding") String embedding,
        @Param("limit") int limit
    );

    @Query(value = """
        SELECT * FROM beschluesse
        WHERE filename ILIKE :filename
        ORDER BY embedding <=> cast(:embedding as vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Beschluss> findByFilenameAndEmbeddingSimilarity(
        @Param("filename") String filename,
        @Param("embedding") String embedding,
        @Param("limit") int limit
    );

    @Query("SELECT DISTINCT b.filename FROM Beschluss b ORDER BY b.filename")
    List<String> findAllFilenames();

    @Modifying
    @Query(value = """
        INSERT INTO beschluesse (id, pdf_url, chunk_index, content, title, topic, filename, word_count, embedding)
        VALUES (:id, :pdfUrl, :chunkIndex, :content, :title, :topic, :filename, :wordCount, cast(:embedding as vector))
        """, nativeQuery = true)
    void insertWithEmbedding(
        @Param("id") UUID id,
        @Param("pdfUrl") String pdfUrl,
        @Param("chunkIndex") Integer chunkIndex,
        @Param("content") String content,
        @Param("title") String title,
        @Param("topic") String topic,
        @Param("filename") String filename,
        @Param("wordCount") Integer wordCount,
        @Param("embedding") String embedding
    );
}
