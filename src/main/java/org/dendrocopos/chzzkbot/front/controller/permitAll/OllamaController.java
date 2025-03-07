package org.dendrocopos.chzzkbot.front.controller.permitAll;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dendrocopos.chzzkbot.ollama.config.OllamaRequest;
import org.dendrocopos.chzzkbot.ollama.config.OllamaResponse;
import org.dendrocopos.chzzkbot.ollama.service.OllamaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Controller
@RequestMapping(value = "/ollama")
@AllArgsConstructor
public class OllamaController {
    private final OllamaService ollamaService;

    @GetMapping(value = "/chat")
    public String ollamaChatGet(Model model){
        return "/html/permitAll/ollama/ollamaChat";
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<OllamaResponse>> OllamaChatPost(@RequestBody OllamaRequest request){
        return ResponseEntity.ok(
            ollamaService.isConnected().flatMapMany(connected -> {
                if(Boolean.TRUE.equals(connected)){
                    return ollamaService.getOllamachatResponse(request.getMessages().getFirst().getContent());
                }else{
                    return Flux.just(OllamaResponse.builder()
                            .message(OllamaResponse.Message.builder()
                                    .content("❌ AI 연결이 되지 않았습니다.")
                                    .build())
                            .build());

                }
            })
        );
    }

}
