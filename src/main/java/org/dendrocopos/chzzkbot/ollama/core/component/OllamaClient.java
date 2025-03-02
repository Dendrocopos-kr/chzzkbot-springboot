package org.dendrocopos.chzzkbot.ollama.core.component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.dendrocopos.chzzkbot.ollama.config.OllamaRequest;
import org.dendrocopos.chzzkbot.ollama.config.OllamaResponse;
import org.dendrocopos.chzzkbot.ollama.config.OllamaResponseProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class OllamaClient {

    private WebClient webClient;
    private final OllamaResponseProcessor responseProcessor;

    @Value("${ollama.baseURL}")
    private String baseURL;

    public OllamaClient(WebClient.Builder webClientBuilder, OllamaResponseProcessor responseProcessor) {
        this.webClient = webClientBuilder.baseUrl(baseURL).build();
        this.responseProcessor = responseProcessor;
    }

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(baseURL)
                .build();
    }

    public boolean isConnected() {
        try {
            HttpResponse<String> response;
            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(baseURL))
                        .build();
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            }
            int statusCode = response.statusCode();
            return (200 <= statusCode && statusCode <= 399);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Mono<String> generateResponse(String userInput) {
        return webClient.post()
                .uri("/api/chat")
                .bodyValue(new OllamaRequest(userInput))
                .retrieve()
                .bodyToFlux(OllamaResponse.class) // 스트리밍 응답을 Flux로 받음
                .collectList() // 리스트로 변환
                .map(responseProcessor::processOllamaResponse); // 메시지를 하나로 합쳐서 반환
    }
}
