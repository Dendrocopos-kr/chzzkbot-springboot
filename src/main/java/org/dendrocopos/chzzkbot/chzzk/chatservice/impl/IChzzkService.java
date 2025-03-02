package org.dendrocopos.chzzkbot.chzzk.chatservice.impl;

import reactor.core.publisher.Mono;

public interface IChzzkService {
    Mono<String> reqChzzk(String path);
    Mono<String> getStatus(String path);
    Mono<String> reqGame(String path);
}
