package com.github.hashjang.hoxton.webflux.config;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.retry.Retry;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class WebClientConfig {
    public static final String SERVICE_PROVIDER = "service-provider";

    @Autowired
    private ReactorLoadBalancerExchangeFilterFunction lbFunction;

    @Bean(SERVICE_PROVIDER)
    public WebClient getWebClient(CircuitBreakerRegistry circuitBreakerRegistry) {
        ConnectionProvider provider = ConnectionProvider.builder(SERVICE_PROVIDER)
                .maxConnections(50).pendingAcquireTimeout(Duration.ofSeconds(5)).build();
        HttpClient httpClient = HttpClient.create(provider)
                .tcpConfiguration(client ->
                        client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
                                .doOnConnected(conn -> conn
                                        .addHandlerLast(new ReadTimeoutHandler(1))
                                        .addHandlerLast(new WriteTimeoutHandler(1))
                                )
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                //Retry在负载均衡前
                .filter(new RetryFilter(SERVICE_PROVIDER))
                .filter(lbFunction)
                //实例级别的断路器需要在负载均衡获取真正地址之后
                .filter(new InstanceCircuitBreakerFilter(SERVICE_PROVIDER, circuitBreakerRegistry))
                .baseUrl("http://" + SERVICE_PROVIDER)
                .build();
    }


    private static class RetryFilter implements ExchangeFilterFunction {
        private final String serviceName;

        private RetryFilter(String serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction exchangeFunction) {
            return exchangeFunction.exchange(clientRequest).retryWhen(Retry.onlyIf(retryContext -> {
                //get请求一定重试
                return clientRequest.method().equals(HttpMethod.GET)
                        //connect Timeout 是一种 IOException
                        || retryContext.exception() instanceof IOException
                        || retryContext.exception() instanceof CallNotPermittedException;
            }).retryMax(1).exponentialBackoff(Duration.ofMillis(100), Duration.ofMillis(1000)));
        }
    }

    private static class InstanceCircuitBreakerFilter implements ExchangeFilterFunction {
        private final String serviceName;
        private final CircuitBreakerRegistry circuitBreakerRegistry;
        ;

        private InstanceCircuitBreakerFilter(String serviceName, CircuitBreakerRegistry circuitBreakerRegistry) {
            this.serviceName = serviceName;
            this.circuitBreakerRegistry = circuitBreakerRegistry;
        }

        @Override
        public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction exchangeFunction) {
            CircuitBreaker circuitBreaker;
            String instancId = clientRequest.url().getHost() + ":" + clientRequest.url().getPort();
            try {
                //使用实例id新建或者获取现有的CircuitBreaker,使用serviceName获取配置
                circuitBreaker = circuitBreakerRegistry.circuitBreaker(instancId, serviceName);
            } catch (ConfigurationNotFoundException e) {
                circuitBreaker = circuitBreakerRegistry.circuitBreaker(instancId);
            }

            return exchangeFunction.exchange(clientRequest).transform(CircuitBreakerOperator.of(circuitBreaker));
        }
    }
}
