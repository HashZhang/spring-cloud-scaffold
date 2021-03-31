package com.github.hashjang.spring.cloud.iiford.service.common.config;

import com.github.hashjang.spring.cloud.iiford.service.common.feign.DefaultErrorDecoder;
import com.github.hashjang.spring.cloud.iiford.service.common.feign.FeignDecoratorBuilderInterceptor;
import feign.Feign;
import feign.codec.ErrorDecoder;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.feign.FeignDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class DefaultOpenFeignConfiguration {
    public static final String FALLBACK_SUFFIX = "_fallback";
    public static final String FALLBACK_FACTORY_SUFFIX = "_fallback_factory";

    @Bean
    public ErrorDecoder errorDecoder() {
        return new DefaultErrorDecoder();
    }

    @Bean
    public Feign.Builder resilience4jFeignBuilder(
            List<FeignDecoratorBuilderInterceptor> feignDecoratorBuilderInterceptors,
            FeignDecorators.Builder builder
    ) {
        feignDecoratorBuilderInterceptors.forEach(feignDecoratorBuilderInterceptor -> feignDecoratorBuilderInterceptor.intercept(builder));
        return Resilience4jFeign.builder(builder.build());
    }

    @Bean
    public FeignDecorators.Builder defaultBuilder(
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
        return FeignDecorators.builder().withRetry(
                retry
        );
    }

}
