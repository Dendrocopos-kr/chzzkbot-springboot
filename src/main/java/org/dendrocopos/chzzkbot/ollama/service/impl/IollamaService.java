package org.dendrocopos.chzzkbot.ollama.service.impl;

import jakarta.servlet.http.HttpSession;
import org.dendrocopos.chzzkbot.ollama.config.OllamaRequest;
import org.dendrocopos.chzzkbot.ollama.config.OllamaResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IollamaService {
    Flux<OllamaResponse> getOllamachatResponse(String sessionId, OllamaRequest request);
    Mono<Boolean> isConnected();
}
