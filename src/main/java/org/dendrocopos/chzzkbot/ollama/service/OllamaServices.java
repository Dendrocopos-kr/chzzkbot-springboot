package org.dendrocopos.chzzkbot.ollama.service;

import lombok.RequiredArgsConstructor;
import org.dendrocopos.chzzkbot.ollama.core.component.OllamaClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OllamaServices {

    private final OllamaClient ollamaClient;

    public Mono<String> getOllamachatResponse(String userInput){
        return ollamaClient.generateResponse(userInput);
    }

    public boolean isConnected(){
        return ollamaClient.isConnected();
    }
}
