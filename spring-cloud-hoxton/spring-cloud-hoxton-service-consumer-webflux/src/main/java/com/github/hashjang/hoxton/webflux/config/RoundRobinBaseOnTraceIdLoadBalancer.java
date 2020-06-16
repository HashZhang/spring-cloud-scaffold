package com.github.hashjang.hoxton.webflux.config;

import brave.Span;
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

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    //这个超时时间，需要设置的比你的请求的 connectTimeout + readTimeout 长
    private final LoadingCache<Long, AtomicInteger> positionCache = Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).build(k -> new AtomicInteger(ThreadLocalRandom.current().nextInt(0, 1000)));
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
        Span currentSpan = tracer.currentSpan();
        //如果没有 traceId，就生成一个新的，但是最好检查下为啥会没有
        //是不是 MQ 消费这种没有主动生成 traceId 的情况，最好主动生成下
        if (currentSpan == null) {
            currentSpan = tracer.newTrace();
        }
        long l = currentSpan.context().traceId();
        int seed = positionCache.get(l).getAndIncrement();
        //这里，serviceInstances可能与上次的内容不同
        //例如上次是实例1，实例2
        //这次是实例2，实例1
        //所以，加上排序，进一步保证不会重试相同实例
        return new DefaultResponse(serviceInstances.stream().sorted(Comparator.comparing(ServiceInstance::getInstanceId)).collect(Collectors.toList()).get(seed % serviceInstances.size()));
    }
}
