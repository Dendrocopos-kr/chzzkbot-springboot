package org.dendrocopos.chzzkbot.ollama.core.component;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dendrocopos.chzzkbot.ollama.config.OllamaMessage;
import org.dendrocopos.chzzkbot.ollama.config.OllamaRequest;
import org.dendrocopos.chzzkbot.ollama.config.OllamaResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OllamaClient {

    private final WebClient webClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${ollama.baseURL}")
    private String baseURL;

    private static final long SESSION_EXPIRY_MINUTES = 30; // ✅ 세션 유지 시간 (30분)

    @Autowired
    public OllamaClient(@Value("${ollama.baseURL}") String baseURL, WebClient.Builder webClientBuilder,RedisTemplate<String, Object> redisTemplate) {
        this.baseURL = baseURL;
        this.webClient = webClientBuilder.baseUrl(this.baseURL).build();
        this.redisTemplate = redisTemplate;
        log.info("✅ OllamaClient 초기화: baseURL = {}", this.baseURL);
    }

    // ✅ 세션별 대화 히스토리 저장 (LinkedList 사용 → FIFO 구조)
    //private final Map<String, LinkedList<OllamaMessage>> chatHistory = new ConcurrentHashMap<>();
    //private static final int MAX_HISTORY_SIZE = 15; // ✅ 최대 대화 개수 제한


    /**
     * ✅ Redis에서 세션별 대화 히스토리 가져오기
     */
    private List<OllamaMessage> getChatHistory(String sessionId) {
        List<OllamaMessage> history = (List<OllamaMessage>) redisTemplate.opsForValue().get(sessionId);
        return history != null ? history : new LinkedList<>();
    }

    /**
     * ✅ Redis에 세션별 대화 히스토리 저장 및 만료 시간 갱신
     */
    private void saveChatHistory(String sessionId, List<OllamaMessage> history) {
        redisTemplate.opsForValue().set(sessionId, history, SESSION_EXPIRY_MINUTES, TimeUnit.MINUTES);
    }

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
        //LinkedList<OllamaMessage> history = chatHistory.computeIfAbsent(sessionId, k -> new LinkedList<>());
        List<OllamaMessage> history = getChatHistory(sessionId);
        log.info("session : {}, history : {}", sessionId, history);

        history.addAll(request.getMessages());

        // ✅ Ollama 요청 객체 생성 (대화 히스토리 포함)
        //OllamaRequest request = new OllamaRequest(new LinkedList<>(history));
        request = OllamaRequest.builder()
                .model(request.getModel())
                .messages(history)
                .stream(request.isStream())
                .build();
        log.info("request : {}", request);

        // ✅ redis에 등록
        saveChatHistory(sessionId, history);

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
                            // ✅ redis에 등록
                            saveChatHistory(sessionId, history);
                            log.info("✅ AI 응답 저장: {}", finalResponse);
                        }
                        responseBuffer.setLength(0); // ✅ 버퍼 초기화
                    }
                })
                .doOnError(error -> log.error("❌ Ollama 응답 오류: {}", error.getMessage()));
    }

}
