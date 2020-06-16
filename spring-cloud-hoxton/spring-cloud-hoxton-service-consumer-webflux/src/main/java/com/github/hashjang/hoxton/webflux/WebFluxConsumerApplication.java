package com.github.hashjang.hoxton.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 不能使用SpringCloudApplication，会自动装载CircuitBreaker
// 不能自动装载CircuitBreaker的原因是因为引入了spring-cloud-starter-netflix-eureka-client依赖
// 这个依赖，包含了hystrix依赖，会自动启用hystrix，我们并不想启用hystrix
// 并且使用SpringCloud的CircuitBreaker的抽象接口，并不能完全使用resilience4j的所有功能
// spring-cloud社区维护的resilience4j的starter功能还有适用性不如resilience4j自己维护的starter
@SpringBootApplication
public class WebFluxConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebFluxConsumerApplication.class, args);
    }
}
