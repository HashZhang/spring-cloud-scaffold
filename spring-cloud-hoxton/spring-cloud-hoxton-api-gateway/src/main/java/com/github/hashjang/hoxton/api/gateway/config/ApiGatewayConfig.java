package com.github.hashjang.hoxton.api.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.event.EnableBodyCachingEvent;
import org.springframework.cloud.gateway.filter.AdaptCachedBodyGlobalFilter;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@EnableConfigurationProperties(ApiGatewayRetryConfig.class)
@LoadBalancerClients(defaultConfiguration = CommonLoadBalancerConfig.class)
public class ApiGatewayConfig {
    @Autowired
    private AdaptCachedBodyGlobalFilter adaptCachedBodyGlobalFilter;
    @Autowired
    private GatewayProperties gatewayProperties;

    @PostConstruct
    public void init() {
        //让每一个路径都做body Cache，这样重试有Body的请求的时候，重试的请求不会没有body，因为原始body是一次性的基于netty的FluxReceive
        gatewayProperties.getRoutes().forEach(routeDefinition -> {
            EnableBodyCachingEvent enableBodyCachingEvent = new EnableBodyCachingEvent(new Object(), routeDefinition.getId());
            adaptCachedBodyGlobalFilter.onApplicationEvent(enableBodyCachingEvent);
        });
    }
}
