package org.dendrocopos.chzzkbot.ollama.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

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
    private boolean stream = true;

    @JsonProperty("options")
    private Map<String, Object> options;

    // ✅ 동적으로 모델명을 주입할 수 있도록 변경
    public OllamaRequest(String model, String userInput) {
        this.model = model; // 모델명 설정
        this.stream = true; // 스트리밍 응답 활성화
        this.options = Map.of(
                "temperature", 0.7,
                "top_p", 0.9
        );

        // 시스템 메시지 및 사용자 입력 메시지를 포함한 리스트 생성
        this.messages = List.of(
                new Message("user", userInput)
        );
    }

    public OllamaRequest(String userInput) {
        this.model = "myuzzoki:latest";
        this.stream = true; // 스트리밍 응답 활성화
        this.options = Map.of(
                "temperature", 0.7,
                "top_p", 0.9
        );

        // 시스템 메시지 및 사용자 입력 메시지를 포함한 리스트 생성
        this.messages = List.of(
                new Message("user", userInput)
        );
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