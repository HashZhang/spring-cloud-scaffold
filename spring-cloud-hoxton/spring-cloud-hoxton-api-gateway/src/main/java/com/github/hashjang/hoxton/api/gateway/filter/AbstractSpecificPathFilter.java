package com.github.hashjang.hoxton.api.gateway.filter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public abstract class AbstractSpecificPathFilter implements GlobalFilter, Ordered {
    private final AntPathMatcher MATCHER = new AntPathMatcher();

    protected final LoadingCache<String, Boolean> FILTER_CACHE = Caffeine.newBuilder().build(key -> {
        String[] split = key.split(",");
        HttpMethod httpMethod = HttpMethod.valueOf(split[0]);
        Map<String, List<HttpMethod>> paths = getPaths();
        for (String path : paths.keySet()) {
            if (MATCHER.match(path, split[1])) {
                List<HttpMethod> httpMethods = paths.get(path);
                if (CollectionUtils.isEmpty(httpMethods)) {
                    return true;
                } else if (httpMethods.contains(httpMethod)) {
                    return true;
                }
            }
        }
        return false;
    });

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (shouldBeFiltered(exchange)) {
            return filter0(exchange, chain);
        }
        return chain.filter(exchange);
    }

    /**
     * 是否要经过这个filter
     *
     * @param exchange
     * @return true则经过，false则跳过
     */
    protected boolean shouldBeFiltered(ServerWebExchange exchange) {
        var request = exchange.getRequest();
        String path = request.getPath().value();
        return FILTER_CACHE.get(request.getMethod() + "," + path);
    }

    protected abstract Mono<Void> filter0(ServerWebExchange exchange, GatewayFilterChain chain);

    protected abstract Map<String, List<HttpMethod>> getPaths();
}