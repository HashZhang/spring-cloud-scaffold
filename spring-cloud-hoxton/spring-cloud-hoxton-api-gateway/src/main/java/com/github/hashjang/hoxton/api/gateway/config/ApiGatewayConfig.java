package com.github.hashjang.hoxton.api.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ApiGatewayRetryConfig.class)
@LoadBalancerClients(defaultConfiguration = CommonLoadBalancerConfig.class)
public class ApiGatewayConfig {

}
