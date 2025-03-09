package org.dendrocopos.chzzkbot.ollama.core.component;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dendrocopos.chzzkbot.ollama.config.OllamaMessage;
import org.dendrocopos.chzzkbot.ollama.config.OllamaRequest;
import org.dendrocopos.chzzkbot.ollama.config.OllamaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OllamaClient {

    private final WebClient webClient;

    @Value("${ollama.baseURL}")
    private String baseURL;

    public OllamaClient(@Value("${ollama.baseURL}") String baseURL, WebClient.Builder webClientBuilder) {
        this.baseURL = baseURL;
        this.webClient = webClientBuilder.baseUrl(this.baseURL).build();
        log.info("✅ OllamaClient 초기화: baseURL = {}", this.baseURL);
    }

    // ✅ 세션별 대화 히스토리 저장 (LinkedList 사용 → FIFO 구조)
    private final Map<String, LinkedList<OllamaMessage>> chatHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 15; // ✅ 최대 대화 개수 제한

    public Mono<Boolean> isConnected() {
        return webClient.get()
                .uri("/")
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .doOnError(error -> log.error("❌ Ollama 서버 연결 실패: {}", error.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * ✅ 세션별 AI 응답 요청 (이전 대화 포함)
     */
    public Flux<OllamaResponse> generateResponse(String sessionId/*, String userInput*/, OllamaRequest request) {
        //String sessionId = session.getId();
        log.info("✅ 사용자 세션 ID: {}", sessionId);

        // ✅ 세션별 기존 대화 내역 불러오기 (없으면 새 LinkedList 생성)
        LinkedList<OllamaMessage> history = chatHistory.computeIfAbsent(sessionId, k -> new LinkedList<>());
        log.info("session : {}, history : {}", sessionId, history);

        // ✅ 새로운 사용자 입력 추가 (최대 개수 초과 시 오래된 데이터 삭제)
        if (history.size() >= MAX_HISTORY_SIZE) {
            history.pollFirst(); // 가장 오래된 메시지 제거
        }
        history.add(request.getMessages().getFirst());

        // ✅ Ollama 요청 객체 생성 (대화 히스토리 포함)
        //OllamaRequest request = new OllamaRequest(new LinkedList<>(history));
        request = OllamaRequest.builder()
                .model(request.getModel())
                .messages(history)
                .stream(request.isStream())
                .build();
        log.info("request : {}", request);

        // ✅ AI 응답을 임시 저장할 StringBuilder
        StringBuilder responseBuffer = new StringBuilder();

        return webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(OllamaResponse.class)
                .doOnNext(response -> {
                    if (response.getMessage() != null && response.getMessage().getContent() != null) {
                        responseBuffer.append(response.getMessage().getContent());
                    }

                    // ✅ `done: true`가 감지되면 최종 문장을 history에 저장
                    if (Boolean.TRUE.equals(response.isDone())) {
                        String finalResponse = responseBuffer.toString().trim();
                        if (!finalResponse.isEmpty()) {
                            history.add(OllamaMessage.builder()
                                    .role("assistant")
                                    .content(finalResponse)
                                    .build());

                            // ✅ 최대 개수 유지
                            if (history.size() > MAX_HISTORY_SIZE) {
                                history.pollFirst();
                            }
                            log.info("✅ AI 응답 저장: {}", finalResponse);
                        }
                        responseBuffer.setLength(0); // ✅ 버퍼 초기화
                    }
                })
                .doOnError(error -> log.error("❌ Ollama 응답 오류: {}", error.getMessage()));
    }

    /**
     * ✅ 세션별 대화 기록 삭제 (사용자 요청 시)
     */
    public void clearChatHistory(HttpSession session) {
        String sessionId = session.getId();
        chatHistory.remove(sessionId);
        log.info("✅ 세션 '{}' 대화 히스토리 초기화됨", sessionId);
    }
}
