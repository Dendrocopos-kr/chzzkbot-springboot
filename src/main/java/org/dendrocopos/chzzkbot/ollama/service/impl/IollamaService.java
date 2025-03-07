package org.dendrocopos.chzzkbot.ollama.service.impl;

import jakarta.servlet.http.HttpSession;
import org.dendrocopos.chzzkbot.ollama.config.OllamaResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IollamaService {
    Flux<OllamaResponse> getOllamachatResponse(HttpSession session, String userInput);
    Mono<Boolean> isConnected();
}
