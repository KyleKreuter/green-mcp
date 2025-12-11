package de.kyle.greenmcp.dto;

public record BeschlussResult(
        String title,
        String topic,
        String content,
        String pdfUrl,
        String filename
    ) {}