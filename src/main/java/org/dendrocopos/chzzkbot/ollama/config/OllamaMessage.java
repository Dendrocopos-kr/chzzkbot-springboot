package org.dendrocopos.chzzkbot.ollama.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OllamaMessage {
    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private String content;
}
