package org.dendrocopos.chzzkbot.chzzk.chatservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ChzzkServices {

    private final WebClient webClient;

    @Value("${chzzk.URL.chzzkBaseURL}")
    String chzzkBaseURL;
    @Value("${chzzk.URL.gameBaseURL}")
    String gameBaseURL;
    @Value("${chzzk.NID_AUT}")
    String nidAut;
    @Value("${chzzk.NID_SES}")
    String nidSes;
    @Value("${spring.application.version}")
    String applicationVersion;
    @Value("${user.agent}")
    String agent;

    public ChzzkServices(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> reqChzzk(String path) {
        return webClient.get()
                .uri(chzzkBaseURL + path)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.COOKIE, "NID_AUT=" + nidAut + ";NID_SES=" + nidSes)
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
                .header(HttpHeaders.COOKIE, "NID_AUT=" + nidAut + ";NID_SES=" + nidSes)
                .retrieve()
                .bodyToMono(String.class);
    }
}
