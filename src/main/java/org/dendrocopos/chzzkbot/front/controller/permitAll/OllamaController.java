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
     * ✅ 채팅 페이지 반환 (타임리프 적용)
     */
    @GetMapping(value = "/chat")
    public String ollamaChatGet(Model model) {
        return "/html/permitAll/ollama/ollamaChat";
    }

    /**
     * ✅ AI 응답 요청 (세션별 대화 내역 포함)
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<OllamaResponse>> OllamaChatPost(@RequestBody OllamaRequest request, HttpSession session) {
        log.info("🔹 세션 ID: {}", session.getId());
        return ResponseEntity.ok(ollamaService.getOllamachatResponse(session.getId(), request ));
    }

    /**
     * ✅ 세션별 대화 기록 초기화 (필요 시 호출 가능)
     */
    @PostMapping(value = "/chat/clear")
    public ResponseEntity<String> clearChatHistory(HttpSession session) {
        ollamaService.clearChatHistory(session);
        return ResponseEntity.ok("✅ 대화 기록이 초기화되었습니다.");
    }
}
