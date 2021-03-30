package com.github.hashjang.spring.cloud.iiford.service.common.config;

import com.github.hashjang.spring.cloud.iiford.service.common.feign.DefaultErrorDecoder;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.codec.ErrorDecoder;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.feign.FeignDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class DefaultOpenFeignConfiguration {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new DefaultErrorDecoder();
    }

    @Bean
    public Feign.Builder resilience4jFeignBuilder(
            Environment environment,
            RetryRegistry retryRegistry
    ) {
        String name = environment.getProperty("feign.client.name");
        Retry retry = null;
        try {
            retry = retryRegistry.retry(name, name);
        } catch (ConfigurationNotFoundException e) {
            retry = retryRegistry.retry(name);
        }
        FeignDecorators decorators = FeignDecorators.builder().withRetry(
                retry
        ).build();
        return Resilience4jFeign.builder(decorators);
    }

    @Bean
    public InvocationHandlerFactory defaultFallbackInvocationHandlerFactory() {

    }
}
