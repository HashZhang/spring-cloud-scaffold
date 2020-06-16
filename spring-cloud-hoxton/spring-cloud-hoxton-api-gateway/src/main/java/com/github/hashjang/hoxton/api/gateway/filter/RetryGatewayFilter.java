package com.github.hashjang.hoxton.api.gateway.filter;

import com.github.hashjang.hoxton.api.gateway.common.CommonConstant;
import com.github.hashjang.hoxton.api.gateway.config.ApiGatewayRetryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RetryGatewayFilter extends RetryGatewayFilterFactory implements GlobalFilter, Ordered {

    private final Map<String, GatewayFilter> gatewayFilterMap = new ConcurrentHashMap<>();
    @Autowired
    private ApiGatewayRetryConfig apiGatewayRetryConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        //获取微服务名称
        String serviceName = request.getHeaders().getFirst(CommonConstant.SERVICE_NAME);
        Map<String, RetryConfig> retryConfigMap = apiGatewayRetryConfig.getRetry();
        //通过微服务名称，获取重试配置
        RetryConfig retryConfig = retryConfigMap.containsKey(serviceName) ? retryConfigMap.get(serviceName) : apiGatewayRetryConfig.getDefault();
        if (retryConfig.getRetries() == 0) {
            return chain.filter(exchange);
        }
        //生成 GatewayFilter,保存到 gatewayFilterMap
        GatewayFilter gatewayFilter = gatewayFilterMap.computeIfAbsent(serviceName, k -> this.apply(retryConfig));
        return gatewayFilter.filter(exchange, chain);
    }

    @Override
    public int getOrder() {
        //必须在RouteToRequestUrlFilter还有LoadBalancerClientFilter之前
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER - 1;
    }
}