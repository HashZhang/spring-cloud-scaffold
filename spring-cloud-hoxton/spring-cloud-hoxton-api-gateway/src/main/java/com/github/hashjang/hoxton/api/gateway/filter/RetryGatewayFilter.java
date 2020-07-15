package com.github.hashjang.hoxton.api.gateway.filter;

import com.github.hashjang.hoxton.api.gateway.common.CommonConstant;
import com.github.hashjang.hoxton.api.gateway.config.ApiGatewayRetryConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
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
    public int getOrder() {
        //必须在RouteToRequestUrlFilter还有LoadBalancerClientFilter之前
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER - 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        //获取微服务名称
        String serviceName = request.getHeaders().getFirst(CommonConstant.SERVICE_NAME);
        Map<String, RetryConfig> retryConfigMap = apiGatewayRetryConfig.getRetry();
        //通过微服务名称，获取重试配置
        RetryConfig retryConfig = retryConfigMap.containsKey(serviceName) ? retryConfigMap.get(serviceName) : apiGatewayRetryConfig.getDefault();
        //重试次数为0，则不重试
        if (retryConfig.getRetries() == 0) {
            return chain.filter(exchange);
        }
        //针对非GET请求，强制限制重试并且只能重试下面的异常b
        HttpMethod method = exchange.getRequest().getMethod();
        if (!HttpMethod.GET.equals(method)) {
            RetryConfig newConfig = new RetryConfig();
            BeanUtils.copyProperties(retryConfig, newConfig);
            newConfig.setSeries();
            newConfig.setStatuses();
            newConfig.setExceptions(//链接超时
                    io.netty.channel.ConnectTimeoutException.class,
                    //No route to host
                    java.net.ConnectException.class,
                    //针对Resilience4j的异常
                    CallNotPermittedException.class);
            retryConfig = newConfig;
        }
        //生成 GatewayFilter,保存到 gatewayFilterMap
        RetryConfig finalRetryConfig = retryConfig;
        GatewayFilter gatewayFilter = gatewayFilterMap.computeIfAbsent(serviceName + ":" + method, k -> this.apply(finalRetryConfig));
        return gatewayFilter.filter(exchange, chain);
    }
}