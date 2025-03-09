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
/*
    // ✅ 모델 미지정 시 기본 모델 사용
    public OllamaRequest(List<OllamaMessage> messages) {
        this("myuzzoki:latest", messages, true);
    }

    // ✅ 사용자가 모델을 지정할 수 있음
    public OllamaRequest(String model, List<OllamaMessage> messages) {
        this(model, messages, true);
    }

    // ✅ 사용자가 모델과 stream 값을 지정할 수 있음
    public OllamaRequest(String model, List<OllamaMessage> messages, boolean stream) {
        this.model = (model == null || model.isBlank()) ? "myuzzoki:latest" : model;
        this.messages = messages;
        this.stream = stream;
        this.options = Map.of("temperature", 0.7, "top_p", 0.9);
    }*/
}
