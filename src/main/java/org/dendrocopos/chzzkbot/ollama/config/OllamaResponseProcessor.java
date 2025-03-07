package org.dendrocopos.chzzkbot.ollama.config;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class OllamaResponseProcessor {

    public Flux<String> processOllamaResponse(Flux<OllamaResponse> responses) {
        return responses
                .map(response -> response.getMessage().getContent()) // ✅ 각 응답에서 message.content 추출
                .filter(content -> content != null && !content.isBlank()) // ✅ null 및 빈 값 제거
                .doOnNext(content -> System.out.println("📌 응답 조각: " + content)); // ✅ 각 응답 로깅
    }
}
