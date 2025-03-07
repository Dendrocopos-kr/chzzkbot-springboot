package org.dendrocopos.chzzkbot.ollama.service;

import lombok.RequiredArgsConstructor;
import org.dendrocopos.chzzkbot.ollama.config.OllamaResponse;
import org.dendrocopos.chzzkbot.ollama.core.component.OllamaClient;
import org.dendrocopos.chzzkbot.ollama.service.impl.IollamaService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OllamaService implements IollamaService {

    private final OllamaClient ollamaClient;

    public Flux<OllamaResponse> getOllamachatResponse(String userInput){
        return ollamaClient.generateResponse(userInput);
    }

    public Mono<Boolean> isConnected(){
        return ollamaClient.isConnected();
    }
}
