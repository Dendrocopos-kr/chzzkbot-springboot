package org.dendrocopos.chzzkbot.core.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CustomWebSocketHandler implements WebSocketHandler {
    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return session.receive()
                .doOnNext(webSocketMessage -> {
                    log.info("Received websocket message: {}", webSocketMessage);
                })
                .concatMap(webSocketMessage -> {
                    log.info("Received websocket message: {}", webSocketMessage);
                    return null;
                })
                .map(o -> session.textMessage("Echo + "+o))
                .then()
                ;

    }
}
