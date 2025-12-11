package de.kyle.greenmcp.tool;

import de.kyle.greenmcp.dto.BeschlussResult;
import de.kyle.greenmcp.entity.Beschluss;
import de.kyle.greenmcp.service.BeschlussService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
 * Unit tests for {@link BeschlussSucheTool}.
 */
@ExtendWith(MockitoExtension.class)
class BeschlussSucheToolTest {

    @Mock
    private BeschlussService beschlussService;

    @InjectMocks
    private BeschlussSucheTool beschlussSucheTool;

    private Beschluss testBeschluss;

    @BeforeEach
    void setUp() {
        testBeschluss = new Beschluss();
        testBeschluss.setId(UUID.randomUUID());
        testBeschluss.setTitle("Klimaschutz Beschluss");
        testBeschluss.setTopic("Umwelt");
        testBeschluss.setContent("Inhalt zum Klimaschutz und erneuerbaren Energien.");
        testBeschluss.setPdfUrl("https://example.com/klimaschutz.pdf");
        testBeschluss.setFilename("klimaschutz-2024.pdf");
        testBeschluss.setChunkIndex(0);
        testBeschluss.setWordCount(50);
    }

    @Nested
    @DisplayName("beschluesseSuchen Tests")
    class BeschluesseSuchenTests {

        @Test
        @DisplayName("should return matching beschluesse as BeschlussResult")
        void shouldReturnMatchingBeschluesse() {
            // Given
            String query = "Klimaschutz";
            Integer limit = 5;
            when(beschlussService.searchBeschluesse(query, limit))
                    .thenReturn(List.of(testBeschluss));

            // When
            List<BeschlussResult> results = beschlussSucheTool.beschluesseSuchen(query, limit);

            // Then
            assertThat(results).hasSize(1);
            BeschlussResult result = results.get(0);
            assertThat(result.title()).isEqualTo(testBeschluss.getTitle());
            assertThat(result.topic()).isEqualTo(testBeschluss.getTopic());
            assertThat(result.content()).isEqualTo(testBeschluss.getContent());
            assertThat(result.pdfUrl()).isEqualTo(testBeschluss.getPdfUrl());
            assertThat(result.filename()).isEqualTo(testBeschluss.getFilename());

            verify(beschlussService).searchBeschluesse(query, limit);
        }

        @Test
        @DisplayName("should return empty list when no matches found")
        void shouldReturnEmptyListWhenNoMatches() {
            // Given
            String query = "NonExistent";
            Integer limit = 5;
            when(beschlussService.searchBeschluesse(query, limit))
                    .thenReturn(Collections.emptyList());

            // When
            List<BeschlussResult> results = beschlussSucheTool.beschluesseSuchen(query, limit);

            // Then
            assertThat(results).isEmpty();
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("should use default limit of 5 when limit is null or invalid")
        void shouldUseDefaultLimitWhenInvalid(Integer limit) {
            // Given
            String query = "Test";
            when(beschlussService.searchBeschluesse(eq(query), eq(5)))
                    .thenReturn(Collections.emptyList());

            // When
            beschlussSucheTool.beschluesseSuchen(query, limit);

            // Then
            verify(beschlussService).searchBeschluesse(query, 5);
        }

        @Test
        @DisplayName("should cap limit at 20")
        void shouldCapLimitAt20() {
            // Given
            String query = "Test";
            Integer limit = 50;
            when(beschlussService.searchBeschluesse(eq(query), eq(20)))
                    .thenReturn(Collections.emptyList());

            // When
            beschlussSucheTool.beschluesseSuchen(query, limit);

            // Then
            verify(beschlussService).searchBeschluesse(query, 20);
        }

        @Test
        @DisplayName("should pass valid limit as is")
        void shouldPassValidLimitAsIs() {
            // Given
            String query = "Test";
            Integer limit = 10;
            when(beschlussService.searchBeschluesse(query, limit))
                    .thenReturn(Collections.emptyList());

            // When
            beschlussSucheTool.beschluesseSuchen(query, limit);

            // Then
            verify(beschlussService).searchBeschluesse(query, 10);
        }

        @Test
        @DisplayName("should handle multiple results")
        void shouldHandleMultipleResults() {
            // Given
            String query = "Umwelt";
            Integer limit = 10;

            Beschluss beschluss2 = new Beschluss();
            beschluss2.setId(UUID.randomUUID());
            beschluss2.setTitle("Umweltschutz Beschluss");
            beschluss2.setTopic("Umwelt");
            beschluss2.setContent("Weiterer Inhalt");
            beschluss2.setPdfUrl("https://example.com/umwelt.pdf");
            beschluss2.setFilename("umwelt-2024.pdf");

            when(beschlussService.searchBeschluesse(query, limit))
                    .thenReturn(List.of(testBeschluss, beschluss2));

            // When
            List<BeschlussResult> results = beschlussSucheTool.beschluesseSuchen(query, limit);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).title()).isEqualTo(testBeschluss.getTitle());
            assertThat(results.get(1).title()).isEqualTo(beschluss2.getTitle());
        }
    }

    @Nested
    @DisplayName("inBeschlussSuchen Tests")
    class InBeschlussSuchenTests {

        @Test
        @DisplayName("should search within specific beschluss")
        void shouldSearchWithinSpecificBeschluss() {
            // Given
            String beschlussName = "klimaschutz";
            String query = "Erneuerbare Energie";
            Integer limit = 5;

            when(beschlussService.searchInBeschluss(beschlussName, query, limit))
                    .thenReturn(List.of(testBeschluss));

            // When
            List<BeschlussResult> results = beschlussSucheTool.inBeschlussSuchen(
                    beschlussName, query, limit);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).title()).isEqualTo(testBeschluss.getTitle());

            verify(beschlussService).searchInBeschluss(beschlussName, query, limit);
        }

        @Test
        @DisplayName("should return empty list when no matches in specific beschluss")
        void shouldReturnEmptyListWhenNoMatchesInBeschluss() {
            // Given
            String beschlussName = "unknown";
            String query = "Test";
            Integer limit = 5;

            when(beschlussService.searchInBeschluss(beschlussName, query, limit))
                    .thenReturn(Collections.emptyList());

            // When
            List<BeschlussResult> results = beschlussSucheTool.inBeschlussSuchen(
                    beschlussName, query, limit);

            // Then
            assertThat(results).isEmpty();
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(ints = {0, -1, -50})
        @DisplayName("should use default limit of 5 when limit is null or invalid")
        void shouldUseDefaultLimitWhenInvalid(Integer limit) {
            // Given
            String beschlussName = "test";
            String query = "Test";
            when(beschlussService.searchInBeschluss(eq(beschlussName), eq(query), eq(5)))
                    .thenReturn(Collections.emptyList());

            // When
            beschlussSucheTool.inBeschlussSuchen(beschlussName, query, limit);

            // Then
            verify(beschlussService).searchInBeschluss(beschlussName, query, 5);
        }

        @Test
        @DisplayName("should cap limit at 20")
        void shouldCapLimitAt20() {
            // Given
            String beschlussName = "test";
            String query = "Test";
            Integer limit = 100;

            when(beschlussService.searchInBeschluss(eq(beschlussName), eq(query), eq(20)))
                    .thenReturn(Collections.emptyList());

            // When
            beschlussSucheTool.inBeschlussSuchen(beschlussName, query, limit);

            // Then
            verify(beschlussService).searchInBeschluss(beschlussName, query, 20);
        }

        @Test
        @DisplayName("should convert results to BeschlussResult correctly")
        void shouldConvertResultsToBeschlussResult() {
            // Given
            String beschlussName = "klimaschutz";
            String query = "Test";
            Integer limit = 5;

            when(beschlussService.searchInBeschluss(beschlussName, query, limit))
                    .thenReturn(List.of(testBeschluss));

            // When
            List<BeschlussResult> results = beschlussSucheTool.inBeschlussSuchen(
                    beschlussName, query, limit);

            // Then
            assertThat(results).hasSize(1);
            BeschlussResult result = results.get(0);
            assertThat(result.title()).isEqualTo(testBeschluss.getTitle());
            assertThat(result.topic()).isEqualTo(testBeschluss.getTopic());
            assertThat(result.content()).isEqualTo(testBeschluss.getContent());
            assertThat(result.pdfUrl()).isEqualTo(testBeschluss.getPdfUrl());
            assertThat(result.filename()).isEqualTo(testBeschluss.getFilename());
        }
    }

    @Nested
    @DisplayName("beschluesseListen Tests")
    class BeschluesseListenTests {

        @Test
        @DisplayName("should return all filenames")
        void shouldReturnAllFilenames() {
            // Given
            List<String> expectedFilenames = List.of(
                    "klimaschutz-2024.pdf",
                    "bildung-2023.pdf",
                    "verkehr-2024.pdf"
            );
            when(beschlussService.getAllFilenames()).thenReturn(expectedFilenames);

            // When
            List<String> filenames = beschlussSucheTool.beschluesseListen();

            // Then
            assertThat(filenames).hasSize(3);
            assertThat(filenames).containsExactlyElementsOf(expectedFilenames);
            verify(beschlussService).getAllFilenames();
        }

        @Test
        @DisplayName("should return empty list when no beschluesse exist")
        void shouldReturnEmptyListWhenNoBeschluesseExist() {
            // Given
            when(beschlussService.getAllFilenames()).thenReturn(Collections.emptyList());

            // When
            List<String> filenames = beschlussSucheTool.beschluesseListen();

            // Then
            assertThat(filenames).isEmpty();
        }

        @Test
        @DisplayName("should return single filename")
        void shouldReturnSingleFilename() {
            // Given
            List<String> expectedFilenames = List.of("single-beschluss.pdf");
            when(beschlussService.getAllFilenames()).thenReturn(expectedFilenames);

            // When
            List<String> filenames = beschlussSucheTool.beschluesseListen();

            // Then
            assertThat(filenames).hasSize(1);
            assertThat(filenames.get(0)).isEqualTo("single-beschluss.pdf");
        }
    }
}
