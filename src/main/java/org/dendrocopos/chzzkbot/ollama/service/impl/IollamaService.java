package org.dendrocopos.chzzkbot.ollama.service.impl;

import reactor.core.publisher.Mono;

public interface IollamaService {
    Mono<String> getOllamachatResponse(String userInput);
    Mono<Boolean> isConnected();
}
