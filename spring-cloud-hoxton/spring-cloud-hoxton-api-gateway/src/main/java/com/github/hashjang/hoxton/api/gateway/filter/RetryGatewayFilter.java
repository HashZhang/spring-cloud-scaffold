package com.github.hashjang.hoxton.api.gateway.filter;

import com.github.hashjang.hoxton.api.gateway.common.CommonConstant;
import com.github.hashjang.hoxton.api.gateway.config.ApiGatewayRetryConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class RetryGatewayFilter extends RetryGatewayFilterFactory implements GlobalFilter, Ordered {

    private final Map<String, GatewayFilter> gatewayFilterMap = new ConcurrentHashMap<>();
    @Autowired
    private ApiGatewayRetryConfig apiGatewayRetryConfig;

    public static void main(String[] args) throws URISyntaxException {
        Path path = Paths.get(RetryGatewayFilter.class.getClassLoader()
                .getResource("test.log").toURI());
        try (Stream<String> lines = Files.lines(path)) {

            List<String[]> originData = lines.map(data -> data.split(" ")).collect(Collectors.toList());

            List<String[]> requests = originData.stream().filter(data -> data[6].contains("CommonLogFilter:42")).collect(Collectors.toList());
            List<String[]> responses = originData.stream().filter(data -> data[6].contains("CommonLogFilter$1:68")).collect(Collectors.toList());

            Map<String, String[]> responsesMap = responses.stream().collect(Collectors.toMap(strings -> strings[4], strings -> strings, (v1, v2) -> v1));

            requests.forEach(request -> {
                Timestamp requestTime = Timestamp.valueOf(request[0] + " " + request[1]);
                String[] response = responsesMap.get(request[4]);
                if (response != null) {
                    Timestamp responseTime = Timestamp.valueOf(response[0] + " " + response[1]);
                    System.out.println(request[1] + ":" + request[4] + ":" + request[7] + ":" + (responseTime.getTime() - requestTime.getTime()));
                } else {
                    System.out.println(request[1] + ":" + request[4] + ":" + request[7]);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Count {
        private String k;
        private long v;
    }
}