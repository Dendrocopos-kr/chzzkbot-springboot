package org.dendrocopos.chzzkbot.ollama.core.component;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dendrocopos.chzzkbot.ollama.config.OllamaRequest;
import org.dendrocopos.chzzkbot.ollama.config.OllamaResponse;
import org.dendrocopos.chzzkbot.ollama.config.OllamaResponseProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Component
public class OllamaClient {

    private final WebClient webClient;
    private final OllamaResponseProcessor responseProcessor;

    @Value("${ollama.baseURL}")
    private String baseURL;

    public OllamaClient(@Value("${ollama.baseURL}") String baseURL, WebClient.Builder webClientBuilder, OllamaResponseProcessor responseProcessor) {
        this.baseURL = baseURL;
        this.webClient = webClientBuilder.baseUrl(this.baseURL).build();
        this.responseProcessor = responseProcessor;

        log.info("✅ OllamaClient 초기화: baseURL = {}", this.baseURL); // ✅ baseURL 확인용 로그 추가
    }


    public Mono<Boolean> isConnected() {
        return webClient.get()
                .uri("/")
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .doOnError(throwable -> log.error("❌ Ollama 서버 연결 실패: {}", throwable.getMessage())) // ✅ 오류 로그 추가
                .onErrorReturn(false); // ✅ 오류 발생 시 false 반환
    }

    public Mono<String> generateResponse(String userInput) {
        log.info("✅ API 요청: baseURL = {}, URI = /api/chat", this.baseURL); // ✅ API 요청 전 URL 확인

        return webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OllamaRequest(userInput))
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("❌ API 호출 오류: HTTP {} - 응답: {}", clientResponse.statusCode(), errorBody);
                                    return Mono.error(new RuntimeException("API 오류: " + errorBody));
                                }))
                .bodyToFlux(OllamaResponse.class)
                .collectList()
                .map(responseProcessor::processOllamaResponse)
                .doOnNext(response -> log.info("✅ 최종 응답: {}", response)); // ✅ 최종 응답 로그 추가
    }

}
