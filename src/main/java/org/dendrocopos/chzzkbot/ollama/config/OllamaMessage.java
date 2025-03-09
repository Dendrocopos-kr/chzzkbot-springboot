package org.dendrocopos.chzzkbot.ollama.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OllamaMessage implements Serializable {
    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private String content;
}
