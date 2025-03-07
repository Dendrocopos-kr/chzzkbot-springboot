package org.dendrocopos.chzzkbot.ollama.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dendrocopos.chzzkbot.ollama.config.OllamaResponse;
import org.dendrocopos.chzzkbot.ollama.core.component.OllamaClient;
import org.dendrocopos.chzzkbot.ollama.service.impl.IollamaService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService implements IollamaService {

    private final OllamaClient ollamaClient;

    /**
     * ✅ 세션별 AI 응답 요청 (이전 대화 내역 포함)
     */
    public Flux<OllamaResponse> getOllamachatResponse(HttpSession session, String userInput) {
        return ollamaClient.generateResponse(session, userInput);
    }

    /**
     * ✅ Ollama 서버 연결 상태 확인
     */
    public Mono<Boolean> isConnected() {
        return ollamaClient.isConnected();
    }

    /**
     * ✅ 세션별 대화 기록 삭제 (사용자 요청 시)
     */
    public void clearChatHistory(HttpSession session) {
        ollamaClient.clearChatHistory(session);
    }
}
