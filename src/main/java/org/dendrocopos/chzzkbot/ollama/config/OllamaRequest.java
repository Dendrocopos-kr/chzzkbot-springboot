package org.dendrocopos.chzzkbot.ollama.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Null 값 제거 (직렬화 문제 해결)
public class OllamaRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("messages")
    private List<Message> messages;

    @JsonProperty("stream")
    private boolean stream = true; // ✅ 기본값을 true로 설정

    @JsonProperty("options")
    private Map<String, Object> options;

    // ✅ 모델 미지정 시 기본 모델 사용
    public OllamaRequest(List<Message> messages) {
        this("myuzzoki:latest", messages, true);
    }

    // ✅ 사용자가 모델과 stream 값을 지정할 수 있음
    public OllamaRequest(String model, List<Message> messages) {
        this(model, messages, true);
    }

    // ✅ 사용자가 모델과 stream 값을 지정할 수 있음
    public OllamaRequest(String model, List<Message> messages, boolean stream) {
        this.model = (model == null || model.isBlank()) ? "myuzzoki:latest" : model;
        this.messages = messages;
        this.stream = stream;
        this.options = Map.of("temperature", 0.7, "top_p", 0.9);
    }

    @Data
    @NoArgsConstructor
    public static class Message {
        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
