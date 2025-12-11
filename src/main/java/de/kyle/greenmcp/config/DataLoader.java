package de.kyle.greenmcp.config;

import de.kyle.greenmcp.entity.Beschluss;
import de.kyle.greenmcp.service.BeschlussService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements ApplicationRunner {

    private final BeschlussService beschlussService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (beschlussService.count() > 0) {
            log.info("Database already contains data, skipping import");
            return;
        }

        log.info("Loading CSV data into database...");

        // Load metadata
        Map<UUID, MetadataEntry> metadataMap = loadMetadata();
        log.info("Loaded {} metadata entries", metadataMap.size());

        // Load embeddings and insert directly with native query
        int count = loadAndInsertEmbeddings(metadataMap);
        log.info("Successfully imported {} Beschluesse into database", count);
    }

    private Map<UUID, MetadataEntry> loadMetadata() throws Exception {
        Map<UUID, MetadataEntry> map = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/metadata.csv").getInputStream(), StandardCharsets.UTF_8))) {

            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = parseCSVLine(line);
                if (parts.length >= 6) {
                    UUID id = UUID.fromString(parts[0].replace("\"", ""));
                    MetadataEntry entry = new MetadataEntry(
                        parts[1].replace("\"", ""),  // filename
                        parts[2].replace("\"", ""),  // title
                        parts[3].replace("\"", ""),  // topic
                        Integer.parseInt(parts[5].replace("\"", ""))  // word_count
                    );
                    map.put(id, entry);
                }
            }
        }
        return map;
    }

    private int loadAndInsertEmbeddings(Map<UUID, MetadataEntry> metadataMap) throws Exception {
        int count = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/embeddings.csv").getInputStream(), StandardCharsets.UTF_8))) {

            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = parseEmbeddingCSVLine(line);
                    if (parts.length >= 5) {
                        UUID id = UUID.fromString(parts[0].replace("\"", ""));
                        String pdfUrl = parts[1].replace("\"", "");
                        int chunkIndex = Integer.parseInt(parts[2].replace("\"", ""));
                        String content = parts[3].replace("\"", "");
                        String embeddingString = parts[4].replace("\"", "").trim();

                        MetadataEntry meta = metadataMap.get(id);

                        Beschluss beschluss = new Beschluss();
                        beschluss.setId(id);
                        beschluss.setPdfUrl(pdfUrl);
                        beschluss.setChunkIndex(chunkIndex);
                        beschluss.setContent(content);

                        if (meta != null) {
                            beschluss.setFilename(meta.filename());
                            beschluss.setTitle(meta.title());
                            beschluss.setTopic(meta.topic());
                            beschluss.setWordCount(meta.wordCount());
                        }

                        beschlussService.insertBeschluss(beschluss, embeddingString);
                        count++;

                        if (count % 500 == 0) {
                            log.info("Imported {} entries...", count);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse line: {}", e.getMessage());
                }
            }
        }
        return count;
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private String[] parseEmbeddingCSVLine(String line) {
        // Special handling for embedding column which contains brackets
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean inBrackets = false;

        for (char c : line.toCharArray()) {
            if (c == '"' && !inBrackets) {
                inQuotes = !inQuotes;
            } else if (c == '[') {
                inBrackets = true;
                current.append(c);
            } else if (c == ']') {
                inBrackets = false;
                current.append(c);
            } else if (c == ',' && !inQuotes && !inBrackets) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private record MetadataEntry(String filename, String title, String topic, int wordCount) {}
}
