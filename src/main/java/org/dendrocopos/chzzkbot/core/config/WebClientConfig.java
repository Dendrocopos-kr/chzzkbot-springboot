package org.dendrocopos.chzzkbot.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return (request, next) -> {
            log.debug("Request: {} {}", request.method(), request.url());
            request.headers().forEach((name, values) -> values.forEach(value -> log.debug(name, value)));
            return next.exchange(request);
        };
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("Response: {}", (clientResponse.statusCode()));
            return Mono.just(clientResponse);
        });
    }

}
