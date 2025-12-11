package de.kyle.greenmcp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmbeddingService}.
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingResponse embeddingResponse;

    @Mock
    private Embedding embedding;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(embeddingModel);
    }

    @Nested
    @DisplayName("embed Tests")
    class EmbedTests {

        @Test
        @DisplayName("should return embedding for given text")
        void shouldReturnEmbeddingForGivenText() {
            // Given
            String text = "Klimaschutz ist wichtig";
            float[] expectedEmbedding = new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f};

            when(embeddingModel.embedForResponse(List.of(text))).thenReturn(embeddingResponse);
            when(embeddingResponse.getResult()).thenReturn(embedding);
            when(embedding.getOutput()).thenReturn(expectedEmbedding);

            // When
            float[] result = embeddingService.embed(text);

            // Then
            assertThat(result).isEqualTo(expectedEmbedding);
            verify(embeddingModel).embedForResponse(List.of(text));
        }

        @Test
        @DisplayName("should handle empty text")
        void shouldHandleEmptyText() {
            // Given
            String text = "";
            float[] expectedEmbedding = new float[]{0.0f, 0.0f, 0.0f};

            when(embeddingModel.embedForResponse(List.of(text))).thenReturn(embeddingResponse);
            when(embeddingResponse.getResult()).thenReturn(embedding);
            when(embedding.getOutput()).thenReturn(expectedEmbedding);

            // When
            float[] result = embeddingService.embed(text);

            // Then
            assertThat(result).isEqualTo(expectedEmbedding);
        }

        @Test
        @DisplayName("should handle long text")
        void shouldHandleLongText() {
            // Given
            String text = "Dies ist ein sehr langer Text ".repeat(100);
            float[] expectedEmbedding = new float[1024];
            for (int i = 0; i < 1024; i++) {
                expectedEmbedding[i] = i * 0.001f;
            }

            when(embeddingModel.embedForResponse(List.of(text))).thenReturn(embeddingResponse);
            when(embeddingResponse.getResult()).thenReturn(embedding);
            when(embedding.getOutput()).thenReturn(expectedEmbedding);

            // When
            float[] result = embeddingService.embed(text);

            // Then
            assertThat(result).hasSize(1024);
            assertThat(result).isEqualTo(expectedEmbedding);
        }

        @Test
        @DisplayName("should handle special characters in text")
        void shouldHandleSpecialCharactersInText() {
            // Given
            String text = "Umlaute: ae oe ue ss und Sonderzeichen: @#$%";
            float[] expectedEmbedding = new float[]{0.5f, 0.6f, 0.7f};

            when(embeddingModel.embedForResponse(List.of(text))).thenReturn(embeddingResponse);
            when(embeddingResponse.getResult()).thenReturn(embedding);
            when(embedding.getOutput()).thenReturn(expectedEmbedding);

            // When
            float[] result = embeddingService.embed(text);

            // Then
            assertThat(result).isEqualTo(expectedEmbedding);
        }
    }

    @Nested
    @DisplayName("toVectorString Tests")
    class ToVectorStringTests {

        @Test
        @DisplayName("should convert float array to vector string format")
        void shouldConvertFloatArrayToVectorString() {
            // Given
            float[] embedding = new float[]{0.1f, 0.2f, 0.3f};

            // When
            String result = embeddingService.toVectorString(embedding);

            // Then
            assertThat(result).isEqualTo("[0.1,0.2,0.3]");
        }

        @Test
        @DisplayName("should handle single element array")
        void shouldHandleSingleElementArray() {
            // Given
            float[] embedding = new float[]{0.5f};

            // When
            String result = embeddingService.toVectorString(embedding);

            // Then
            assertThat(result).isEqualTo("[0.5]");
        }

        @Test
        @DisplayName("should handle empty array")
        void shouldHandleEmptyArray() {
            // Given
            float[] embedding = new float[]{};

            // When
            String result = embeddingService.toVectorString(embedding);

            // Then
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("should handle negative values")
        void shouldHandleNegativeValues() {
            // Given
            float[] embedding = new float[]{-0.1f, 0.2f, -0.3f};

            // When
            String result = embeddingService.toVectorString(embedding);

            // Then
            assertThat(result).isEqualTo("[-0.1,0.2,-0.3]");
        }

        @Test
        @DisplayName("should handle very small values")
        void shouldHandleVerySmallValues() {
            // Given
            float[] embedding = new float[]{1.0E-7f, 2.0E-8f};

            // When
            String result = embeddingService.toVectorString(embedding);

            // Then
            assertThat(result).startsWith("[");
            assertThat(result).endsWith("]");
            assertThat(result).contains(",");
        }

        @Test
        @DisplayName("should handle large array (1024 dimensions)")
        void shouldHandleLargeArray() {
            // Given
            float[] embedding = new float[1024];
            for (int i = 0; i < 1024; i++) {
                embedding[i] = i * 0.001f;
            }

            // When
            String result = embeddingService.toVectorString(embedding);

            // Then
            assertThat(result).startsWith("[");
            assertThat(result).endsWith("]");
            // Count commas - should be 1023 for 1024 elements
            long commaCount = result.chars().filter(ch -> ch == ',').count();
            assertThat(commaCount).isEqualTo(1023);
        }

        @Test
        @DisplayName("should produce valid PostgreSQL vector format")
        void shouldProduceValidPostgresVectorFormat() {
            // Given
            float[] embedding = new float[]{0.123f, 0.456f, 0.789f};

            // When
            String result = embeddingService.toVectorString(embedding);

            // Then
            // PostgreSQL vector format: [val1,val2,val3]
            assertThat(result).matches("\\[[-\\d.,E]+\\]");
        }
    }
}
