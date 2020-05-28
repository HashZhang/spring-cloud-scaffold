package com.github.hashjang.hoxton.api.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "spring.cloud.gateway")
public class ApiGatewayRetryConfig {
    private Map<String, RetryGatewayFilterFactory.RetryConfig> retry;

    public RetryGatewayFilterFactory.RetryConfig getDefault() {
        return retry.computeIfAbsent("default", key -> new RetryGatewayFilterFactory.RetryConfig());
    }
}
