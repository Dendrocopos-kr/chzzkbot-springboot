package org.dendrocopos.chzzkbot.ollama.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // Null 값 제거 (직렬화 문제 해결)
@Builder
public class OllamaRequest {

    @JsonProperty("model")
    @Builder.Default
    private String model = "myuzzoki:latest";

    @JsonProperty("messages")
    private List<OllamaMessage> messages;

    @JsonProperty("stream")
    @Builder.Default
    private boolean stream = true; // ✅ 기본값을 true로 설정

    @JsonProperty("options")
    @Builder.Default
    private Map<String, Object> options = Map.of("temperature", 0.7, "top_p", 0.9);
}
