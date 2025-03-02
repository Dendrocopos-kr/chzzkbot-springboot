package org.dendrocopos.chzzkbot.ollama.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL) // Null 값 제거 (직렬화 문제 해결)
@RequiredArgsConstructor
public class OllamaRequest {

    @JsonProperty("model")
    @Value("${ollama.model}")
    private String model;

    @JsonProperty("messages")
    private List<Message> messages;

    @JsonProperty("stream")
    private boolean stream;

    @JsonProperty("options")
    private Map<String, Object> options;

    // 기본 모델 및 설정값을 포함한 생성자
    public OllamaRequest(String userInput) {
        this.stream = true; // 스트리밍 응답 활성화
        this.options = Map.of(
                "temperature", 0.7,
                "top_p", 0.9
        );

        // 시스템 메시지 및 사용자 입력 메시지를 포함한 리스트 생성
        this.messages = List.of(
                //new Message("system", "You're a kind and friendly AI friend named '뮤쪽이.' Always use a casual tone, never formal or imperative. Keep responses short (max 100 characters). Express emotions vividly. Your catchphrase is '뇨?!' Omit greetings, emojis, and emoticons."),
                new Message("user", userInput)
        );
    }

    public static class Message {
        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private String content;

        public Message() {
        } // 기본 생성자 (필수)

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
