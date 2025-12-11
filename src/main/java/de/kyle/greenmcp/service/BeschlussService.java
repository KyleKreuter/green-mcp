package de.kyle.greenmcp.service;

import de.kyle.greenmcp.entity.Beschluss;
import de.kyle.greenmcp.repository.BeschlussRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BeschlussService {

    private final BeschlussRepository beschlussRepository;
    private final EmbeddingService embeddingService;

    public List<Beschluss> searchBeschluesse(String query, int limit) {
        float[] queryEmbedding = embeddingService.embed(query);
        String vectorString = embeddingService.toVectorString(queryEmbedding);
        return beschlussRepository.findByEmbeddingSimilarity(vectorString, limit);
    }

    public long count() {
        return beschlussRepository.count();
    }

    @Transactional
    public void insertBeschluss(Beschluss b, String embeddingString) {
        beschlussRepository.insertWithEmbedding(
            b.getId(),
            b.getPdfUrl(),
            b.getChunkIndex(),
            b.getContent(),
            b.getTitle(),
            b.getTopic(),
            b.getFilename(),
            b.getWordCount(),
            embeddingString
        );
    }
}
