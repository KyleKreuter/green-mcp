package de.kyle.greenmcp.config;

import de.kyle.greenmcp.tool.BeschlussSucheTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider beschlussSucheToolProvider(BeschlussSucheTool beschlussSucheTool) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(beschlussSucheTool)
            .build();
    }
}
