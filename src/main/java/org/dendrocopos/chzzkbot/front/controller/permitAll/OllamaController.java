package org.dendrocopos.chzzkbot.front.controller.permitAll;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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

@Slf4j
@Controller
@RequestMapping(value = "/ollama")
@RequiredArgsConstructor
public class OllamaController {

    private final OllamaService ollamaService;

    /**
     * ✅ /ollama/chat GET 요청을 메인 페이지로 리디렉션
     */
    @GetMapping("/chat")
    public String redirectToHome() {
        return "redirect:/";
    }


    /**
     * ✅ AI 응답 요청 (세션별 대화 내역 포함)
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<OllamaResponse>> OllamaChatPost(@RequestBody OllamaRequest request, HttpSession session) {
        log.info("🔹 세션 ID: {}", session.getId());
        return ResponseEntity.ok(ollamaService.getOllamachatResponse(session.getId(), request ));
    }
}
