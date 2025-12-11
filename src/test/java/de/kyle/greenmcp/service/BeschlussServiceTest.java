package de.kyle.greenmcp.service;

import de.kyle.greenmcp.entity.Beschluss;
import de.kyle.greenmcp.repository.BeschlussRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BeschlussService}.
 */
@ExtendWith(MockitoExtension.class)
class BeschlussServiceTest {

    @Mock
    private BeschlussRepository beschlussRepository;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private BeschlussService beschlussService;

    private Beschluss testBeschluss;
    private float[] testEmbedding;
    private String testVectorString;

    @BeforeEach
    void setUp() {
        testBeschluss = new Beschluss();
        testBeschluss.setId(UUID.randomUUID());
        testBeschluss.setTitle("Test Beschluss");
        testBeschluss.setTopic("Klimaschutz");
        testBeschluss.setContent("Test Inhalt zum Klimaschutz");
        testBeschluss.setFilename("klimaschutz-2024.pdf");
        testBeschluss.setPdfUrl("https://example.com/klimaschutz.pdf");
        testBeschluss.setChunkIndex(0);
        testBeschluss.setWordCount(100);

        testEmbedding = new float[]{0.1f, 0.2f, 0.3f};
        testVectorString = "[0.1,0.2,0.3]";
    }

    @Nested
    @DisplayName("searchBeschluesse Tests")
    class SearchBeschluesseTests {

        @Test
        @DisplayName("should return beschluesse matching the query")
        void shouldReturnBeschluesseMatchingQuery() {
            // Given
            String query = "Klimaschutz";
            int limit = 5;
            List<Beschluss> expectedResults = List.of(testBeschluss);

            when(embeddingService.embed(query)).thenReturn(testEmbedding);
            when(embeddingService.toVectorString(testEmbedding)).thenReturn(testVectorString);
            when(beschlussRepository.findByEmbeddingSimilarity(testVectorString, limit))
                    .thenReturn(expectedResults);

            // When
            List<Beschluss> results = beschlussService.searchBeschluesse(query, limit);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).isEqualTo(testBeschluss);

            verify(embeddingService).embed(query);
            verify(embeddingService).toVectorString(testEmbedding);
            verify(beschlussRepository).findByEmbeddingSimilarity(testVectorString, limit);
        }

        @Test
        @DisplayName("should return empty list when no matches found")
        void shouldReturnEmptyListWhenNoMatchesFound() {
            // Given
            String query = "NonExistentTopic";
            int limit = 5;

            when(embeddingService.embed(query)).thenReturn(testEmbedding);
            when(embeddingService.toVectorString(testEmbedding)).thenReturn(testVectorString);
            when(beschlussRepository.findByEmbeddingSimilarity(anyString(), anyInt()))
                    .thenReturn(Collections.emptyList());

            // When
            List<Beschluss> results = beschlussService.searchBeschluesse(query, limit);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should pass correct limit to repository")
        void shouldPassCorrectLimitToRepository() {
            // Given
            String query = "Test";
            int limit = 10;

            when(embeddingService.embed(query)).thenReturn(testEmbedding);
            when(embeddingService.toVectorString(testEmbedding)).thenReturn(testVectorString);
            when(beschlussRepository.findByEmbeddingSimilarity(anyString(), eq(limit)))
                    .thenReturn(Collections.emptyList());

            // When
            beschlussService.searchBeschluesse(query, limit);

            // Then
            verify(beschlussRepository).findByEmbeddingSimilarity(testVectorString, limit);
        }
    }

    @Nested
    @DisplayName("searchInBeschluss Tests")
    class SearchInBeschlussTests {

        @Test
        @DisplayName("should search within specific beschluss by filename")
        void shouldSearchWithinSpecificBeschluss() {
            // Given
            String filename = "klimaschutz";
            String query = "Erneuerbare Energie";
            int limit = 5;
            List<Beschluss> expectedResults = List.of(testBeschluss);

            when(embeddingService.embed(query)).thenReturn(testEmbedding);
            when(embeddingService.toVectorString(testEmbedding)).thenReturn(testVectorString);
            when(beschlussRepository.findByFilenameAndEmbeddingSimilarity(
                    "%" + filename + "%", testVectorString, limit))
                    .thenReturn(expectedResults);

            // When
            List<Beschluss> results = beschlussService.searchInBeschluss(filename, query, limit);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).isEqualTo(testBeschluss);
        }

        @Test
        @DisplayName("should add wildcards to filename for ILIKE query")
        void shouldAddWildcardsToFilename() {
            // Given
            String filename = "wahlprogramm";
            String query = "Test";
            int limit = 3;

            when(embeddingService.embed(query)).thenReturn(testEmbedding);
            when(embeddingService.toVectorString(testEmbedding)).thenReturn(testVectorString);
            when(beschlussRepository.findByFilenameAndEmbeddingSimilarity(anyString(), anyString(), anyInt()))
                    .thenReturn(Collections.emptyList());

            // When
            beschlussService.searchInBeschluss(filename, query, limit);

            // Then
            verify(beschlussRepository).findByFilenameAndEmbeddingSimilarity(
                    "%wahlprogramm%", testVectorString, limit);
        }

        @Test
        @DisplayName("should return empty list when no matches in specific beschluss")
        void shouldReturnEmptyListWhenNoMatchesInBeschluss() {
            // Given
            String filename = "unknown";
            String query = "Test";
            int limit = 5;

            when(embeddingService.embed(query)).thenReturn(testEmbedding);
            when(embeddingService.toVectorString(testEmbedding)).thenReturn(testVectorString);
            when(beschlussRepository.findByFilenameAndEmbeddingSimilarity(anyString(), anyString(), anyInt()))
                    .thenReturn(Collections.emptyList());

            // When
            List<Beschluss> results = beschlussService.searchInBeschluss(filename, query, limit);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllFilenames Tests")
    class GetAllFilenamesTests {

        @Test
        @DisplayName("should return all distinct filenames")
        void shouldReturnAllDistinctFilenames() {
            // Given
            List<String> expectedFilenames = List.of(
                    "klimaschutz-2024.pdf",
                    "bildung-2023.pdf",
                    "verkehr-2024.pdf"
            );
            when(beschlussRepository.findAllFilenames()).thenReturn(expectedFilenames);

            // When
            List<String> filenames = beschlussService.getAllFilenames();

            // Then
            assertThat(filenames).hasSize(3);
            assertThat(filenames).containsExactlyElementsOf(expectedFilenames);
            verify(beschlussRepository).findAllFilenames();
        }

        @Test
        @DisplayName("should return empty list when no beschluesse exist")
        void shouldReturnEmptyListWhenNoBeschluesseExist() {
            // Given
            when(beschlussRepository.findAllFilenames()).thenReturn(Collections.emptyList());

            // When
            List<String> filenames = beschlussService.getAllFilenames();

            // Then
            assertThat(filenames).isEmpty();
        }
    }

    @Nested
    @DisplayName("count Tests")
    class CountTests {

        @Test
        @DisplayName("should return total count of beschluesse")
        void shouldReturnTotalCount() {
            // Given
            long expectedCount = 42L;
            when(beschlussRepository.count()).thenReturn(expectedCount);

            // When
            long count = beschlussService.count();

            // Then
            assertThat(count).isEqualTo(expectedCount);
            verify(beschlussRepository).count();
        }

        @Test
        @DisplayName("should return zero when no beschluesse exist")
        void shouldReturnZeroWhenNoBeschluesseExist() {
            // Given
            when(beschlussRepository.count()).thenReturn(0L);

            // When
            long count = beschlussService.count();

            // Then
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("insertBeschluss Tests")
    class InsertBeschlussTests {

        @Test
        @DisplayName("should insert beschluss with embedding")
        void shouldInsertBeschlussWithEmbedding() {
            // Given
            String embeddingString = "[0.1,0.2,0.3]";

            // When
            beschlussService.insertBeschluss(testBeschluss, embeddingString);

            // Then
            verify(beschlussRepository).insertWithEmbedding(
                    testBeschluss.getId(),
                    testBeschluss.getPdfUrl(),
                    testBeschluss.getChunkIndex(),
                    testBeschluss.getContent(),
                    testBeschluss.getTitle(),
                    testBeschluss.getTopic(),
                    testBeschluss.getFilename(),
                    testBeschluss.getWordCount(),
                    embeddingString
            );
        }
    }
}
