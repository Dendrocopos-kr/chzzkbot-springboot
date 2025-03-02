package org.dendrocopos.chzzkbot.ollama.config;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OllamaResponseProcessor {

    public String processOllamaResponse(List<OllamaResponse> responses) {
        // 응답에서 "message.content" 값을 모두 합쳐서 하나의 문장으로 변환
        return responses.stream()
                .map(response -> response.getMessage().getContent()) // 메시지의 content 추출
                .collect(Collectors.joining()) // 모든 문자 합치기
                .trim(); // 앞뒤 공백 제거
    }
}
