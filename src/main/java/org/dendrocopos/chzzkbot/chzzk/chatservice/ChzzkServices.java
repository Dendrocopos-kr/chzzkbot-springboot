package org.dendrocopos.chzzkbot.chzzk.chatservice;

import lombok.RequiredArgsConstructor;
import org.dendrocopos.chzzkbot.chzzk.chatservice.impl.IChzzkService;
import org.dendrocopos.chzzkbot.chzzk.nid.nid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ChzzkServices implements IChzzkService {

    private final WebClient webClient;

    @Value("${chzzk.URL.chzzkBaseURL}")
    String chzzkBaseURL;
    @Value("${chzzk.URL.gameBaseURL}")
    String gameBaseURL;
    @Value("${spring.application.version}")
    String applicationVersion;
    @Value("${user.agent}")
    String agent;

    public Mono<String> reqChzzk(String path) {
        return webClient.get()
                .uri(chzzkBaseURL + path)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.COOKIE, "NID_AUT=" + nid.NID_AUT + ";NID_SES=" + nid.NID_SES)
                .header(HttpHeaders.USER_AGENT, agent)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> getStatus(String path) {
        return reqChzzk(path);
    }

    public Mono<String> reqGame(String path) {
        return webClient.get()
                .uri(gameBaseURL + path)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.COOKIE, "NID_AUT=" + nid.NID_AUT + ";NID_SES=" + nid.NID_SES)
                .retrieve()
                .bodyToMono(String.class);
    }
}
