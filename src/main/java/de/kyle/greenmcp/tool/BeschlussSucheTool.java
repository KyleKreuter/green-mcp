package de.kyle.greenmcp.tool;

import de.kyle.greenmcp.dto.BeschlussResult;
import de.kyle.greenmcp.entity.Beschluss;
import de.kyle.greenmcp.service.BeschlussService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BeschlussSucheTool {

    private final BeschlussService beschlussService;

    @Tool(description = "Sucht nach Beschlüssen der Grünen Hamburg basierend auf einer semantischen Suchanfrage. Gibt die relevantesten Beschlüsse mit Titel, Thema, Inhalt und PDF-Link zurück.")
    public List<BeschlussResult> beschluesseSuchen(
        @ToolParam(description = "Die Suchanfrage in natürlicher Sprache, z.B. 'Klimaschutz' oder 'Bildungspolitik'") String query,
        @ToolParam(description = "Anzahl der zurückzugebenden Ergebnisse (1-20, Standard: 5)") Integer limit
    ) {
        int effectiveLimit = (limit == null || limit < 1) ? 5 : Math.min(limit, 20);

        List<Beschluss> results = beschlussService.searchBeschluesse(query, effectiveLimit);

        return results.stream()
            .map(b -> new BeschlussResult(
                b.getTitle(),
                b.getTopic(),
                b.getContent(),
                b.getPdfUrl(),
                b.getFilename()
            ))
            .toList();
    }

    @Tool(description = "Sucht innerhalb eines bestimmten Beschlusses (PDF-Datei) nach relevanten Passagen. Nutze dieses Tool, wenn du gezielt in einem spezifischen Beschluss suchen möchtest.")
    public List<BeschlussResult> inBeschlussSuchen(
        @ToolParam(description = "Der Dateiname des Beschlusses (oder Teil davon), z.B. 'Klimaschutz' oder '2024-Wahlprogramm'") String beschlussName,
        @ToolParam(description = "Die Suchanfrage in natürlicher Sprache") String query,
        @ToolParam(description = "Anzahl der zurückzugebenden Ergebnisse (1-20, Standard: 5)") Integer limit
    ) {
        int effectiveLimit = (limit == null || limit < 1) ? 5 : Math.min(limit, 20);

        List<Beschluss> results = beschlussService.searchInBeschluss(beschlussName, query, effectiveLimit);

        return results.stream()
            .map(b -> new BeschlussResult(
                b.getTitle(),
                b.getTopic(),
                b.getContent(),
                b.getPdfUrl(),
                b.getFilename()
            ))
            .toList();
    }

    @Tool(description = "Listet alle verfügbaren Beschlüsse (PDF-Dateien) auf. Nutze dieses Tool, um herauszufinden, welche Beschlüsse durchsucht werden können.")
    public List<String> beschluesseListen() {
        return beschlussService.getAllFilenames();
    }

}
