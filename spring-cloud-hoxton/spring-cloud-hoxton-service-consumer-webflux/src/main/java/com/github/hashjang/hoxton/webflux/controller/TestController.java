package com.github.hashjang.hoxton.webflux.controller;

import com.github.hashjang.hoxton.webflux.config.WebClientConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

@Log4j2
@RestController
public class TestController {
    @Resource(name = WebClientConfig.SERVICE_PROVIDER)
    private WebClient webClient;

    @RequestMapping("/testGetTimeOut")
    public Mono<String> testGetTimeOut() {
        return webClient.get().uri("/test-read-time-out")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {
                });
    }

    @RequestMapping("/testPostTimeOut")
    public Mono<String> testPostTimeOut() {
        return webClient.post().uri("/test-read-time-out")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<>() {
                });
    }
}
