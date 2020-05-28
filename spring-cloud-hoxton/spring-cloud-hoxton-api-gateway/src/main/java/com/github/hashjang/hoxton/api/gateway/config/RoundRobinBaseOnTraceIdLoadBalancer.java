package com.github.hashjang.hoxton.api.gateway.config;

import brave.Tracer;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.reactive.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Round-Robin-based implementation of {@link org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer}.
 * 同名同路径类，用于替换框架中的类
 * 框架中的RoundRobinLoadBalancer可能会导致在同一个请求重试的时候，两次重试返回的都是同一个实例
 * 参考我提的issues：https://github.com/spring-cloud/spring-cloud-commons/issues/747
 * 并且由于 BlackingLoadBalancerClient 通过 Mono.blocking() 的方式调用的这里的 choose，线程并不固定，和源线程无关，所以ThreadLocal没用
 * 唯一不变的是traceId，通过这个traceId来保证同一个traceId内的position每次重试加1
 */
@Log4j2
public class RoundRobinBaseOnTraceIdLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    private final LoadingCache<Long, AtomicInteger> positionCache = Caffeine.newBuilder().expireAfterWrite(3, TimeUnit.SECONDS).build(k -> new AtomicInteger(ThreadLocalRandom.current().nextInt(0, 1000)));
    private final String serviceId;
    private final ServiceInstanceListSupplier serviceInstanceListSupplier;
    private final Tracer tracer;

    public RoundRobinBaseOnTraceIdLoadBalancer(String serviceId, ServiceInstanceListSupplier serviceInstanceListSupplier, Tracer tracer) {
        this.serviceId = serviceId;
        this.serviceInstanceListSupplier = serviceInstanceListSupplier;
        this.tracer = tracer;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        return serviceInstanceListSupplier.get().next().map(this::getInstanceResponse);
    }

    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> serviceInstances) {
        if (serviceInstances.isEmpty()) {
            log.warn("No servers available for service: " + this.serviceId);
            return new EmptyResponse();
        }
        long l = tracer.currentSpan().context().traceId();
        int seed = positionCache.get(l).getAndIncrement();
        return new DefaultResponse(serviceInstances.get(seed % serviceInstances.size()));
    }
}
